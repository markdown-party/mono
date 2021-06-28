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
  private val events = mutableByteGapBufferOf()
  private val eventsIds = mutableEventIdentifierGapBufferOf()
  private val eventsSizes = mutableIntGapBufferOf()

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
      until: Int
  ) {
    requireRange(from, until, array) { "event out of range" }

    val id = EventIdentifier(seqno, site)
    events.pushAtGap(array, from, until)
    events.gap.shift(-(until - from))
    eventsIds.pushAtGap(id)
    eventsIds.gap.shift(-1)
    eventsSizes.pushAtGap(until - from)
    eventsSizes.gap.shift(-1)
  }

  /**
   * Appends the provided change to the store of changes. The given change will be appended at the
   * current index of changes.
   *
   * @param array the bytes that compose the change.
   * @param from where the bytes should be read.
   * @param until where the bytes should be read.
   */
  private fun change(array: ByteArray, from: Int, until: Int) {
    requireRange(from, until, array) { "change out of range" }
    check(eventsIds.gap.startIndex > 0) { "missing event for change insertion" }

    val id = eventsIds[eventsIds.gap.startIndex - 1]
    changes.pushAtGap(array, from = from, until = until)
    changesIds.pushAtGap(id)
    changesSizes.pushAtGap(until - from)
  }

  /**
   * Returns true if the insertion of an event with [seqno] and [site] would require moving back in
   * the log. If the event could be inserted at the current index, `true` will be returned.
   */
  private fun shouldInsertBefore(seqno: SequenceNumber, site: SiteIdentifier): Boolean {
    if (eventsIds.gap.startIndex == 0) return false
    val id = eventsIds[eventsIds.gap.startIndex - 1]
    return EventIdentifier(seqno, site) <= id
  }

  /**
   * Returns true iff the current cursor index points at an event with the given [seqno] and [site]
   * identifier.
   */
  private fun containsAtCursor(seqno: SequenceNumber, site: SiteIdentifier): Boolean {
    if (eventsIds.gap.startIndex == eventsIds.size) return false
    val id = eventsIds[eventsIds.gap.startIndex]
    return EventIdentifier(seqno, site) == id
  }

  /**
   * Moves the event cursor back, dismissing all the changes associated with the current event if
   * needed.
   */
  private fun backward() {
    check(eventsIds.gap.startIndex > 0) { "Can't move backward if at start." }
    val removed = eventsIds[eventsIds.gap.startIndex - 1]
    val size = eventsSizes[eventsSizes.gap.startIndex - 1]
    val from = events.gap.startIndex - size
    val until = events.gap.startIndex

    // Remove the event.
    events.remove(from, size)
    eventsIds.remove(eventsIds.gap.startIndex - 1)
    eventsSizes.remove(eventsSizes.gap.startIndex - 1)

    // Remove all the associated changes.
    while (changesIds.gap.startIndex != 0 && changesIds[changesIds.gap.startIndex - 1] == removed) {
      val changeSize = changesSizes[changesSizes.gap.startIndex - 1]
      val changeFrom = changes.gap.startIndex - changeSize
      val changeUntil = changes.gap.startIndex

      changes.remove(changeFrom, changeSize)
      changesIds.remove(changesIds.gap.startIndex - 1)
      changesSizes.remove(changesSizes.gap.startIndex - 1)

      // Update the current projection.
      current =
          projection.backward(
              model = current,
              identifier = removed,
              data = events.backing,
              from = from,
              until = until,
              changeData = changes.backing,
              changeFrom = changeFrom,
              changeUntil = changeUntil,
          )
    }
  }

  /**
   * Returns `true` iff there are some events at the current event cursor. In this case, the events
   * will be and changes to the projection performed.
   */
  private fun shouldApplyChanges(): Boolean {
    return eventsIds.size != 0 && eventsIds.gap.startIndex < eventsIds.size
  }

  /**
   * Moves the projection forward, meaning that the event at the current cursor index will be used
   * to generate some changes and move the aggregated value forward.
   */
  private fun forwardChanges() {
    val id = eventsIds[eventsIds.gap.startIndex]
    val size = eventsSizes[eventsSizes.gap.startIndex]
    val from = events.gap.startIndex
    val until = from + size

    // Shift the event log.
    events.gap.shift(size)
    eventsIds.gap.shift(1)
    eventsSizes.gap.shift(1)

    // Update the current value.
    current =
        with(projection) {
          scope.forward(
              model = current,
              identifier = id,
              data = events.backing,
              from = from,
              until = until,
          )
        }
  }

  override val size: Int
    get() = eventsIds.size

  override fun contains(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Boolean {
    return acknowledged.contains(seqno, site)
  }

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
    check(events.gap.startIndex == events.size) { "cursor should be at end" }
    check(eventsIds.gap.startIndex == eventsIds.size) { "cursor should be at end" }
    check(eventsSizes.gap.startIndex == eventsSizes.size) { "cursor should be at end" }

    // Fast return.
    if (acknowledged.contains(seqno, site)) return

    // Insert the event.
    while (shouldInsertBefore(seqno, site)) backward()
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
    check(events.gap.startIndex == events.size) { "cursor should be at end" }
    check(eventsIds.gap.startIndex == eventsIds.size) { "cursor should be at end" }
    check(eventsSizes.gap.startIndex == eventsSizes.size) { "cursor should be at end" }

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

  override fun acknowledge(from: MutableEventLog): MutableEventLog {
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

  override fun iterator(): EventIterator = Iterator()

  override fun merge(from: MutableEventLog): MutableEventLog {
    val iterator = from.iterator()
    // TODO : Only reverse up to the from lower bound.
    // TODO : Optimize to avoid "backward-then-forward" for each event.
    while (iterator.hasPrevious()) iterator.movePrevious()
    while (iterator.hasNext()) {
      iterator.moveNext()
      insert(
          seqno = iterator.seqno,
          site = iterator.site,
          event = iterator.event,
          from = iterator.from,
          until = iterator.until,
      )
    }
    return this
  }

  override fun clear() {
    events.clear()
    eventsIds.clear()
    eventsSizes.clear()
    changes.clear()
    changesIds.clear()
    changesSizes.clear()
  }

  override var current: T = initial
    private set

  /**
   * An inner class which can be used to iterate over the items from a [MutableHistoryImpl],
   * providing list-like access to the items of the [MutableEventLog].
   */
  private inner class Iterator : EventIterator {

    /**
     * The current position in the event identifiers log. This lets us know "which" event we're
     * pointing at.
     */
    private var cursorIdsIndex: Int = eventsIds.size

    /**
     * The current position in the event sizes identifiers. This lets us know "what data" the
     * currently pointed event contains, since not all events have an identical size.
     */
    private var cursorEvents: Int = events.size

    override val seqno: SequenceNumber
      get() = eventsIds[cursorIdsIndex].seqno

    override val site: SiteIdentifier
      get() = eventsIds[cursorIdsIndex].site

    override val event: ByteArray
      get() = events.backing

    override val from: Int
      get() = cursorEvents

    override val until: Int
      get() = from + eventsSizes[cursorIdsIndex]

    override fun hasNext(): Boolean {
      return cursorIdsIndex < eventsIds.size - 1
    }

    override fun hasPrevious(): Boolean {
      return cursorIdsIndex > 0
    }

    override fun nextIndex(): Int {
      check(hasNext()) { "No next element." }
      return cursorIdsIndex + 1
    }

    override fun previousIndex(): Int {
      check(hasPrevious()) { "No previous element." }
      return cursorIdsIndex - 1
    }

    override fun moveNext() {
      check(hasNext()) { "No next element" }
      cursorEvents += eventsSizes[cursorIdsIndex]
      cursorIdsIndex++
    }

    override fun movePrevious() {
      check(hasPrevious()) { "No previous element." }
      cursorIdsIndex--
      cursorEvents -= eventsSizes[cursorIdsIndex]
    }
  }
}
