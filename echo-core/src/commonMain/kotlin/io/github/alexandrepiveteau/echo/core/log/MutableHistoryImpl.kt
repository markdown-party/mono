package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.mutableByteGapBufferOf
import io.github.alexandrepiveteau.echo.core.buffer.mutableEventIdentifierGapBufferOf
import io.github.alexandrepiveteau.echo.core.buffer.mutableIntGapBufferOf
import io.github.alexandrepiveteau.echo.core.buffer.pushAtGap
import io.github.alexandrepiveteau.echo.core.causality.*
import io.github.alexandrepiveteau.echo.core.requireRange

/**
 * An implementation of a [MutableHistory], with an initial aggregate, and a projection that is used
 * to incrementally update the model.
 */
internal class MutableHistoryImpl<T>(
    initial: T,
    private val projection: MutableProjection<T>,
) : MutableHistory<T> {

  // The ChangeScope that will be provided to the projection whenever some changes mush be appended
  // to the changes history.
  private val scope = ChangeScope(this::change)

  // Store what we've already seen.
  private val acknowledged = MutableAcknowledgeMap()

  // Storing the events.
  private val eventStore = BlockLog()

  // Storing the changes.
  private val changes = mutableByteGapBufferOf()
  private val changesIds = mutableEventIdentifierGapBufferOf()
  private val changesSizes = mutableIntGapBufferOf()

  /**
   * Appends the provided event to the store of events. The given event will be appended at the
   * current index of events.
   *
   * @param seqno the sequence number of the event.
   * @param site the site identifier of the event.
   * @param array the bytes that compose the event.
   * @param from where the bytes should be read.
   * @param until where the bytes should be read.
   */
  private fun event(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      array: ByteArray,
      from: Int,
      until: Int,
  ) {
    eventStore.pushAtGapWithoutMove(
        id = EventIdentifier(seqno, site),
        array = array,
        from = from,
        until = until,
    )
  }

  /**
   * Appends the provided change to the store of changes. The given change will be appended at the
   * current index of changes.
   *
   * @param array the bytes that compose the change.
   * @param from where the bytes should be read.
   * @param until where the bytes should be read.
   */
  private fun change(
      array: ByteArray,
      from: Int,
      until: Int,
  ) {
    requireRange(from, until, array) { "change out of range" }

    changes.pushAtGap(array, from = from, until = until)
    changesIds.pushAtGap(eventStore.lastId)
    changesSizes.pushAtGap(until - from)
  }

  /**
   * Returns true if the insertion of an event with [seqno] and [site] would require moving back in
   * the log. If the event could be inserted at the current index, `true` will be returned.
   */
  private fun shouldInsertBefore(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Boolean {
    // TODO : Make this a single condition.
    if (!eventStore.hasPrevious) return false
    return EventIdentifier(seqno, site) <= eventStore.lastId
  }

  /**
   * Moves the event cursor back, dismissing all the changes associated with the current event if
   * needed.
   */
  private fun moveCursorLeft() {
    check(eventStore.hasPrevious) { "Can't move backward if at start." }

    // Remove all the associated changes.
    reverseChange@ while (changesIds.gap.startIndex > 0) {
      val changeId = changesIds[changesIds.gap.startIndex - 1]
      if (changeId != eventStore.lastId) break@reverseChange

      val changeSize = changesSizes[changesSizes.gap.startIndex - 1]
      val changeFrom = changes.gap.startIndex - changeSize
      val changeUntil = changes.gap.startIndex

      // Update the current projection.
      current =
          projection.backward(
              model = current,
              identifier = eventStore.lastId,
              data = eventStore.backing,
              from = eventStore.lastFrom,
              until = eventStore.lastUntil,
              changeData = changes.backing,
              changeFrom = changeFrom,
              changeUntil = changeUntil,
          )

      // Only remove the change once it has been used to update the projection.
      changes.remove(changeFrom, changeSize)
      changesIds.remove(changesIds.gap.startIndex - 1)
      changesSizes.remove(changesSizes.gap.startIndex - 1)
    }

    // Move the event to the right of the cursor. The event cursor is shifted only after it has been
    // reversed with all the changes, to ensure that the content is read (and not the gap, which may
    // have unreliable data).
    eventStore.moveLeft()
  }

  /**
   * Returns `true` iff there are some events at the current event cursor. In this case, the events
   * will be and changes to the projection performed.
   */
  private fun shouldApplyChanges(): Boolean = eventStore.hasNext

  /**
   * Moves the projection forward, meaning that the event at the current cursor index will be used
   * to generate some changes and move the aggregated value forward.
   */
  private fun forwardChanges() {
    eventStore.moveRight()

    val id = eventStore.lastId
    val from = eventStore.lastFrom
    val until = eventStore.lastUntil

    // Update the current value.
    current =
        with(projection) {
          scope.forward(
              model = current,
              identifier = id,
              data = eventStore.backing,
              from = from,
              until = until,
          )
        }
  }

  override val size: Int
    get() = eventStore.size

  override fun contains(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Boolean = acknowledged.contains(seqno, site)

  override fun insert(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int
  ) {

    // Input sanitization.
    require(seqno.isSpecified)
    require(site.isSpecified)
    requireRange(from, until, event)

    // State checks
    check(!eventStore.hasNext) { "cursor should be at end" }

    // Fast return.
    if (acknowledged.contains(seqno, site)) return

    // Insert the event.
    acknowledge(seqno, site)
    while (shouldInsertBefore(seqno, site)) moveCursorLeft()
    event(seqno, site, event, from, until)
    while (shouldApplyChanges()) forwardChanges()
  }

  override fun append(
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int
  ): EventIdentifier {

    // Input requirements
    require(site.isSpecified) { "site must be specified" }
    requireRange(from, until, event) { "event out of bounds" }

    // State checks
    check(!eventStore.hasNext) { "cursor should be at end" }

    // Calculate the sequence number that will be attributed to the event.
    val seqno = acknowledged.expected()
    val id = EventIdentifier(seqno, site)

    // Insert the event.
    acknowledge(seqno, site)
    event(seqno = id.seqno, site = site, array = event, from = from, until = until)
    forwardChanges()

    // Return the identifier of the inserted item.
    return id
  }

  override fun acknowledge(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ) {
    acknowledged.acknowledge(seqno, site)
  }

  override fun acknowledge(
      from: MutableEventLog,
  ): MutableEventLog {
    // TODO : Make this O(n) or O(log(n) * n) rather than O(n*n)
    val iterator = from.acknowledged().iterator()
    while (iterator.hasNext()) {
      val (seqno, site) = iterator.nextEventIdentifier()
      acknowledge(seqno, site)
    }
    return this
  }

  override fun acknowledged(): EventIdentifierArray {
    return acknowledged.toEventIdentifierArray()
  }

  override fun iterator(): EventIterator = eventStore.Iterator()

  override fun merge(
      from: EventLog,
  ): MutableEventLog {

    // Fast success.
    if (from.size == 0) return this

    val iterator = from.iterator()
    while (iterator.hasPrevious()) iterator.movePrevious()

    // TODO : Only reverse up to the from lower bound.
    // TODO : Optimize to avoid "backward-then-forward" for each event.
    var keepGoing = true
    while (keepGoing) {
      insert(
          seqno = iterator.seqno,
          site = iterator.site,
          event = iterator.event,
          from = iterator.from,
          until = iterator.until,
      )
      keepGoing = iterator.hasNext()
      if (keepGoing) iterator.moveNext()
    }
    return this
  }

  override fun clear() {
    eventStore.clear()
    changes.clear()
    changesIds.clear()
    changesSizes.clear()
  }

  override var current: T = initial
    private set
}
