package io.github.alexandrepiveteau.echo.core.log

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

  // Storing the events and the changes.
  private val eventStore = BlockLog()
  private val eventStoreBySite = mutableMapOf<SiteIdentifier, BlockLog>()
  private val changeStore = BlockLog()

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
    changeStore.pushAtGap(
        id = eventStore.lastId,
        array = array,
        from = from,
        until = until,
    )
  }

  /**
   * Returns true if the insertion of an event with [seqno] and [site] would require moving back in
   * the log. If the event could be inserted at the current index, `true` will be returned.
   */
  private fun shouldInsertBefore(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Boolean {
    return eventStore.hasPrevious && EventIdentifier(seqno, site) <= eventStore.lastId
  }

  /**
   * Moves the event cursor back, dismissing all the changes associated with the current event if
   * needed.
   */
  private fun moveCursorLeft() {
    check(eventStore.hasPrevious) { "Can't move backward if at start." }

    // Remove all the associated changes.
    reverseChange@ while (changeStore.hasPrevious) {

      // val changeId = changesIds[changesIds.gap.startIndex - 1]
      if (changeStore.lastId != eventStore.lastId) break@reverseChange

      // Update the current projection.
      current =
          projection.backward(
              model = current,
              identifier = eventStore.lastId,
              data = eventStore.backing,
              from = eventStore.lastFrom,
              until = eventStore.lastUntil,
              changeData = changeStore.backing,
              changeFrom = changeStore.lastFrom,
              changeUntil = changeStore.lastUntil,
          )

      // Only remove the change once it has been used to update the projection.
      changeStore.removeLeft()
    }

    // Move the event to the right of the cursor. The event cursor is shifted only after it has been
    // reversed with all the changes, to ensure that the content is read (and not the gap, which may
    // have unreliable data).
    eventStore.moveLeft()
  }

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
    eventStore.pushAtGapWithoutMove(
        id = EventIdentifier(seqno, site),
        array = event,
        from = from,
        until = until,
    )
    eventStoreBySite
        .getOrPut(site) { BlockLog() }
        .pushAtId(
            id = EventIdentifier(seqno, site),
            array = event,
            from = from,
            until = until,
        )
    while (eventStore.hasNext) forwardChanges()
  }

  override fun append(
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int
  ): EventIdentifier {
    val seqno = acknowledged.expected()
    insert(
        site = site,
        seqno = seqno,
        event = event,
        from = from,
        until = until,
    )
    return EventIdentifier(seqno, site)
  }

  override fun acknowledge(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Unit = acknowledged.acknowledge(seqno, site)

  override fun acknowledge(
      from: EventLog,
  ): MutableEventLog {
    acknowledged.acknowledge(from.acknowledged())
    return this
  }

  override fun acknowledged(): EventIdentifierArray = acknowledged.toEventIdentifierArray()

  override fun iterator(): EventIterator = eventStore.Iterator()

  override fun iterator(
      site: SiteIdentifier,
  ): EventIterator {
    require(site.isSpecified) { "Site must be specified." }
    return eventStoreBySite[site]?.Iterator() ?: EmptyEventIterator
  }

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
    eventStoreBySite.clear()
    changeStore.clear()
  }

  override var current: T = initial
    private set
}
