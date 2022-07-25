package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.copyOfRange
import io.github.alexandrepiveteau.echo.core.causality.*
import io.github.alexandrepiveteau.echo.core.log.EventLog.OnLogUpdateListener
import io.github.alexandrepiveteau.echo.core.requireRange
import kotlinx.datetime.Clock

/**
 * An implementation of [MutableEventLog], which stores events by site as well as in a linear
 * fashion, allowing for faster queries.
 *
 * @param clock the [Clock] used to integrate new events.
 */
abstract class AbstractMutableEventLog(
    clock: Clock = Clock.System,
) : MutableEventLog {

  /** The [OnLogUpdateListener] which should be notified when the log is updated. */
  private val listeners = mutableSetOf<OnLogUpdateListener>()

  // Store what we've already seen.
  private val acknowledgedMap = MutableAcknowledgeMap(clock)

  // Storing the events and the changes.
  internal val eventStore = BlockLog()
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
    while (hasPrevious() && EventIdentifier(previousSeqno, previousSite) > id) movePrevious()
    while (hasNext() && EventIdentifier(nextSeqno, nextSite) < id) moveNext()
    add(seqno, site, event, from, until)
  }

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
    eventStore.iteratorAtEnd().addOrdered(seqno, site, event, from, until)
    site(site).iteratorAtEnd().addOrdered(seqno, site, event, from, until)
    notifyLogListeners()
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
    notifyLogListeners()
  }

  override fun acknowledge(from: EventLog): MutableEventLog {
    acknowledgedMap.acknowledge(from.acknowledged())
    notifyLogListeners()
    return this
  }

  override fun acknowledged(): EventIdentifierArray = acknowledgedMap.toEventIdentifierArray()

  override fun iterator(): EventIterator = eventStore.iteratorAtEnd()

  override fun iterator(site: SiteIdentifier): EventIterator {
    return site(site).iteratorAtEnd()
  }

  override fun merge(from: EventLog): MutableEventLog {
    val iterator = from.iterator()
    while (iterator.hasPrevious()) iterator.movePrevious()
    while (iterator.hasNext()) {
      iterator.moveNext()
      insert(
          seqno = iterator.previousSeqno,
          site = iterator.previousSite,
          event = iterator.previousEvent.copyOfRange(iterator.previousFrom, iterator.previousUntil),
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
        if (EventIdentifier(iterator.nextSeqno, iterator.nextSite) == id) {
          iterator.remove()
          removed = true
          break@loop
        }
      }
    }

    return removed
  }

  override fun clear() {
    eventStore.clear()
    eventStoreBySite.clear()
    notifyLogListeners()
  }

  /** Notifies all the [OnLogUpdateListener]s that a change occurred. */
  private fun notifyLogListeners() = listeners.toSet().forEach(OnLogUpdateListener::onLogUpdated)

  override fun registerLogUpdateListener(listener: OnLogUpdateListener) {
    listeners += listener
    listener.onLogUpdated()
  }

  override fun unregisterLogUpdateListener(listener: OnLogUpdateListener) {
    listeners -= listener
  }
}
