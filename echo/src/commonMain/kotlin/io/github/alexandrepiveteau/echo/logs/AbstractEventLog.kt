package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier

/** A skeletal implementation of [EventLog], which provides some utilities. */
abstract class AbstractEventLog<out T> : EventLog<T> {
  override fun contains(site: SiteIdentifier, seqno: SequenceNumber) = get(site, seqno) != null
}
