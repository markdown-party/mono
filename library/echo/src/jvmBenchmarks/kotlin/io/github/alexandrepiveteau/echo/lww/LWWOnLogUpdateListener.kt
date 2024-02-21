package io.github.alexandrepiveteau.echo.lww

import io.github.alexandrepiveteau.echo.core.buffer.mutableEventIdentifierGapBufferOf
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.EventLog
import io.github.alexandrepiveteau.echo.core.log.MutableEventLog

/**
 * An implementation of [EventLog.OnLogUpdateListener] which will remove all the events from a
 * [MutableEventLog] except for the last one.
 *
 * @param log the [MutableEventLog] in which the events are removed.
 */
class LWWOnLogUpdateListener(private val log: MutableEventLog) : EventLog.OnLogUpdateListener {

  override fun onInsert(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      data: ByteArray,
      from: Int,
      until: Int
  ) {
    // Preserve the last event.
    val iterator = log.iteratorAtEnd()
    if (iterator.hasPrevious()) iterator.movePrevious()

    // Buffer all the events which should be removed.
    val removed = mutableEventIdentifierGapBufferOf()
    while (iterator.hasPrevious()) {
      removed.push(iterator.previousEventIdentifier)
      iterator.movePrevious()
    }

    // Remove all the events.
    for (i in 0 until removed.size) {
      val (removedSeqno, removedSite) = removed[i]
      log.remove(removedSeqno, removedSite)
    }
  }
}
