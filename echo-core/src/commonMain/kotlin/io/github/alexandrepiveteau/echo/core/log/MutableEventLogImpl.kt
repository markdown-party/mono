package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.*
import io.github.alexandrepiveteau.echo.core.requireRange

/**
 * An implementation of [MutableEventLog], which stores events by site as well as in a linear
 * fashion, allowing for faster queries.
 */
internal open class MutableEventLogImpl : MutableEventLog {

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
    if (acknowledgedMap.contains(seqno, site)) return

    // Insert the event.
    acknowledge(seqno, site)
    while (shouldInsertBefore(seqno, site)) moveLeft()
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
    while (eventStore.hasNext) moveRight()
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
  }
}
