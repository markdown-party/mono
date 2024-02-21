package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.copyOfRange
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.EventLog.OnLogUpdateListener
import io.github.alexandrepiveteau.echo.core.log.buffer.MutableAcknowledgeMap
import kotlinx.datetime.Clock

/**
 * A base implementation of [MutableEventLog], which provides a [MutableAcknowledgeMap] and handles
 * registration and un-registration of the [OnLogUpdateListener]s.
 *
 * @param clock the [Clock] used to integrate new events.
 */
internal abstract class AbstractMutableEventLog(clock: Clock = Clock.System) : MutableEventLog {

  // Listeners management.

  /** The [OnLogUpdateListener] which should be notified when the log is updated. */
  private val listeners = mutableSetOf<OnLogUpdateListener>()

  /**
   * Notifies all the [OnLogUpdateListener]s that a change occurred.
   *
   * @param block the [block] to execute.
   */
  protected fun forEachLogUpdateListener(
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

  // Acknowledgements management.

  /** A [MutableAcknowledgeMap] which should be used to acknowledge new events. */
  private val acknowledgements = MutableAcknowledgeMap(clock)

  final override fun append(
      site: SiteIdentifier,
      event: ByteArray,
      from: Int,
      until: Int,
  ): EventIdentifier {
    val seqno = acknowledgements.expected()
    insert(
        site = site,
        seqno = seqno,
        event = event,
        from = from,
        until = until,
    )
    return EventIdentifier(seqno, site)
  }

  final override fun contains(seqno: SequenceNumber, site: SiteIdentifier): Boolean =
      acknowledgements.contains(seqno, site)

  final override fun acknowledge(seqno: SequenceNumber, site: SiteIdentifier) {
    acknowledgements.acknowledge(seqno, site)
    forEachLogUpdateListener { onAcknowledgement() }
  }

  final override fun acknowledge(from: EventLog): MutableEventLog {
    acknowledgements.acknowledge(from.acknowledged())
    forEachLogUpdateListener { onAcknowledgement() }
    return this
  }

  final override fun acknowledged(): EventIdentifierArray =
      acknowledgements.toEventIdentifierArray()

  final override fun merge(from: EventLog): MutableEventLog {
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
}
