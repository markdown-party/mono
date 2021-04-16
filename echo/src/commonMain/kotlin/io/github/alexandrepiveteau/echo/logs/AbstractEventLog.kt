package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier

/** A skeletal implementation of [EventLog], which provides some utilities. */
abstract class AbstractEventLog<out T> : EventLog<T> {
  override fun contains(site: SiteIdentifier, seqno: SequenceNumber) = get(site, seqno) != null
  override fun iterator(): Iterator<EventLog.Entry<T>> = eventIterator()
  override fun eventIterator(): EventIterator<EventLog.Entry<T>> =
      eventIterator(
          site = SiteIdentifier(0),
          seqno = SequenceNumber.Zero,
      )
}
