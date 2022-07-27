package io.github.alexandrepiveteau.echo.core.log.buffer

import io.github.alexandrepiveteau.echo.core.buffer.copyOfRange
import io.github.alexandrepiveteau.echo.core.causality.*
import io.github.alexandrepiveteau.echo.core.log.*
import io.github.alexandrepiveteau.echo.core.log.EventLog.OnLogUpdateListener
import io.github.alexandrepiveteau.echo.core.requireRange
import kotlinx.datetime.Clock

/**
 * An implementation of [MutableEventLog], which stores events by site as well as in a linear
 * fashion, allowing for faster queries.
 *
 * @param clock the [Clock] used to integrate new events.
 */
internal abstract class AbstractGapBufferMutableEventLog(
    clock: Clock = Clock.System,
) : MutableEventLog {

  /** The [OnLogUpdateListener] which should be notified when the log is updated. */
  private val listeners = mutableSetOf<OnLogUpdateListener>()

  // Store what we've already seen.
  private val acknowledgedMap = MutableAcknowledgeMap(clock)

  // Storing the events and the changes.
  private val eventStore = BlockLog()
  private val eventStoreBySite = mutableMapOf<SiteIdentifier, BlockLog>()

  override val size: Int
    get() = eventStore.size

  /**
   * Retrieves the [BlockLog] for the given [SiteIdentifier].
   *
   * @param site the [SiteIdentifier] for which the [BlockLog] is retrieved.
   * @return the [BlockLog] for the given site.
   */
  private fun site(site: SiteIdentifier) = eventStoreBySite.getOrPut(site) { BlockLog() }

  /**
   * Moves the [MutableEventIterator] to the right index for insertion of the given event, assuming
   * all the events are linearly ordered.
   *
   * @see MutableEventIterator.add
   */
  private fun MutableEventIterator.addOrdered(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int,
  ) {
    val id = EventIdentifier(seqno, site)
    while (hasPrevious() && previousEventIdentifier > id) movePrevious()
    while (hasNext() && nextEventIdentifier < id) moveNext()
    add(seqno, site, event, from, until)
  }

  /**
   * Adds an event to the log, starting from the end of the [EventLog].
   *
   * @receiver the [MutableEventIterator], starting from the end.
   */
  protected open fun MutableEventIterator.addToLog(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int,
  ) = addOrdered(seqno, site, event, from, until)

  override fun insert(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int,
  ) {

    // Input sanitization.
    require(seqno.isSpecified)
    requireRange(from, until, event)

    // Don't add existing events.
    if (EventIdentifier(seqno, site) in this) return

    // Acknowledge the new event.
    acknowledge(seqno, site)

    // Adding the event in the iterators.
    eventStore.iteratorAtEnd().addToLog(seqno, site, event, from, until)
    site(site).iteratorAtEnd().addOrdered(seqno, site, event, from, until)
    notifyLogListeners { onInsert(seqno, site, event, from, until) }
  }

  override fun contains(seqno: SequenceNumber, site: SiteIdentifier): Boolean =
      acknowledgedMap.contains(seqno, site)

  override fun append(
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int,
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

  override fun acknowledge(seqno: SequenceNumber, site: SiteIdentifier) {
    acknowledgedMap.acknowledge(seqno, site)
    notifyLogListeners { onAcknowledgement() }
  }

  override fun acknowledge(from: EventLog): MutableEventLog {
    acknowledgedMap.acknowledge(from.acknowledged())
    notifyLogListeners { onAcknowledgement() }
    return this
  }

  override fun acknowledged(): EventIdentifierArray = acknowledgedMap.toEventIdentifierArray()

  override fun iterator(): EventIterator = eventStore.iterator()

  override fun iteratorAtEnd(): EventIterator = eventStore.iteratorAtEnd()

  override fun iterator(site: SiteIdentifier): EventIterator = site(site).iterator()

  override fun iteratorAtEnd(site: SiteIdentifier): EventIterator = site(site).iteratorAtEnd()

  override fun merge(from: EventLog): MutableEventLog {
    // TODO : Perform the insertion in a single pass, without calling `insert` :
    //
    // Outline :
    // 1. Use both acknowledged() to figure out the common bound, by taking the minimum for both
    //    sites, but only where the sites differ (not sure about this one ?).
    // 2. Find out the smallest sequence number in the resulting array. Move the current log to this
    //    position.
    // 3. Perform some step-by-step iteration over both logs, inserting missing events from the new
    //    log. Once all the events have been inserted, notify the listeners.
    val inserted = from.iterator()
    while (inserted.hasNext()) {
      inserted.moveNext()
      insert(
          seqno = inserted.previousSeqno,
          site = inserted.previousSite,
          event = inserted.previousEvent.copyOfRange(inserted.previousFrom, inserted.previousUntil),
      )
    }
    return this
  }

  override fun remove(seqno: SequenceNumber, site: SiteIdentifier): Boolean {

    // Input sanitization.
    require(seqno.isSpecified)

    val id = EventIdentifier(seqno, site)
    var removed = false

    val iterators = setOf(eventStore.iteratorAtEnd(), site(site).iteratorAtEnd())
    for (iterator in iterators) {
      loop@ while (iterator.hasPrevious()) {
        iterator.movePrevious()
        if (iterator.nextEventIdentifier == id) {
          iterator.remove()
          removed = true
          break@loop
        }
      }
    }

    if (removed) notifyLogListeners { onRemoved(seqno, site) }

    return removed
  }

  override fun clear() {
    eventStore.clear()
    eventStoreBySite.clear()
    notifyLogListeners { onCleared() }
  }

  /**
   * Notifies all the [OnLogUpdateListener]s that a change occurred.
   *
   * @param block the [block] to execute.
   */
  private fun notifyLogListeners(
      block: OnLogUpdateListener.() -> Unit,
  ) = listeners.toSet().forEach(block)

  override fun registerLogUpdateListener(listener: OnLogUpdateListener) {
    listeners += listener
    listener.onRegistered()
  }

  override fun unregisterLogUpdateListener(listener: OnLogUpdateListener) {
    listeners -= listener
    listener.onUnregistered()
  }
}
