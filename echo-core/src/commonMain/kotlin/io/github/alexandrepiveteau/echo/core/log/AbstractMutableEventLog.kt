package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.copyOfRange
import io.github.alexandrepiveteau.echo.core.causality.*
import io.github.alexandrepiveteau.echo.core.requireRange

/**
 * An implementation of [MutableEventLog], which stores events by site as well as in a linear
 * fashion, allowing for faster queries.
 */
abstract class AbstractMutableEventLog : MutableEventLog {

  // Store what we've already seen.
  private val acknowledgedMap = MutableAcknowledgeMap()

  // Storing the events and the changes.
  internal val eventStore = BlockLog()
  private val eventStoreBySite = mutableMapOf<SiteIdentifier, BlockLog>()

  override val size: Int
    get() = eventStore.size

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
   * Returns true if the insertion of an event with [seqno] and [site] would require moving forward
   * the log. This looks at the current log value (if available). If the event could be inserted
   * after, `true` will be returned.
   */
  private fun shouldInsertAfter(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Boolean {
    return eventStore.hasCurrent && EventIdentifier(seqno, site) > eventStore.currentId
  }

  /**
   * Moves the event cursor back, dismissing all the changes associated with the current event if
   * needed.
   */
  open fun moveLeft() {
    check(eventStore.hasPrevious) { "Can't move backward if at start." }

    // Move the event to the right of the cursor. The event cursor is shifted only after it has been
    // reversed with all the changes, to ensure that the content is read (and not the gap, which may
    // have unreliable data).
    eventStore.moveLeft()
  }

  /**
   * Moves the projection forward, meaning that the event at the current cursor index will be used
   * to generate some changes and move the aggregated value forward.
   */
  open fun moveRight() {
    eventStore.moveRight()
  }

  /**
   * Pushes the given event at the current gap position, without moving it. This also populates the
   * site-specific [BlockLog] with the given event.
   */
  private fun pushAtGapWithoutMove(
      id: EventIdentifier,
      array: ByteArray,
      from: Int = 0,
      until: Int = array.size,
  ) {
    eventStore.pushAtGapWithoutMove(
        id = id,
        array = array,
        from = from,
        until = until,
    )
    eventStoreBySite
        .getOrPut(id.site) { BlockLog() }
        .pushAtId(
            id = id,
            array = array,
            from = from,
            until = until,
        )
  }

  /**
   * Removes the event at the given gap position, without moving it. Because of the removal, the
   * next event will then be place directly at the current cursor position.
   */
  private fun removeAtGapWithoutMove() {
    val id = eventStore.currentId
    eventStore.removeCurrent()
    eventStoreBySite[id.site]?.removeById(id)
  }

  /**
   * Partially inserts the given event, but does not move the gap afterwards. This method does not
   * make the assumption that the event may be inserted at the current index. Rather, it will
   * iterate to the right / left until it may insert the event, and push it without moving the gap.
   */
  protected open fun partialInsert(
      id: EventIdentifier,
      array: ByteArray,
      from: Int = 0,
      until: Int = array.size
  ) {
    // Fast return.
    if (contains(id.seqno, id.site)) return

    // Insert the event.
    acknowledge(id.seqno, id.site)
    while (shouldInsertBefore(id.seqno, id.site)) moveLeft()
    while (shouldInsertAfter(id.seqno, id.site)) moveRight()
    pushAtGapWithoutMove(id, array, from, until)
  }

  /**
   * Partially removes the given event, but does not move the gap afterwards. This method does not
   * make the assumption that it is correctly positioned on the event. Rather, it will move left and
   * right until it finds the event to remove, and only then remove it without moving the gap.
   */
  protected open fun partialRemove(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Boolean {
    // Fast return.
    if (!contains(seqno, site)) return false

    // Move the right index.
    while (shouldInsertBefore(seqno, site)) moveLeft()
    while (shouldInsertAfter(seqno, site)) moveRight()
    removeAtGapWithoutMove()
    return true
  }

  private fun resetCursor() {
    while (eventStore.hasCurrent) moveRight()
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
    check(!eventStore.hasCurrent) { "cursor should be at end" }

    partialInsert(EventIdentifier(seqno, site), event, from, until).apply { resetCursor() }
  }

  override fun contains(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Boolean = acknowledgedMap.contains(seqno, site)

  override fun append(
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int
  ): EventIdentifier {
    val seqno = acknowledgedMap.expected()
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
  ): Unit = acknowledgedMap.acknowledge(seqno, site)

  override fun acknowledge(
      from: EventLog,
  ): MutableEventLog {
    acknowledgedMap.acknowledge(from.acknowledged())
    return this
  }

  override fun acknowledged(): EventIdentifierArray = acknowledgedMap.toEventIdentifierArray()

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

    var keepGoing = true
    while (keepGoing) {
      partialInsert(
          id = EventIdentifier(iterator.seqno, iterator.site),
          array = iterator.event.copyOfRange(iterator.from, iterator.until),
      )
      keepGoing = iterator.hasNext()
      if (keepGoing) iterator.moveNext()
    }

    resetCursor()
    return this
  }

  override fun remove(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Boolean {

    // Input sanitization.
    require(seqno.isSpecified)
    require(site.isSpecified)

    return partialRemove(seqno, site).apply { resetCursor() }
  }

  override fun clear() {
    eventStore.clear()
    eventStoreBySite.clear()
  }
}
