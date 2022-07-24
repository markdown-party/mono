package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.core.buffer.MutableEventIdentifierGapBuffer
import io.github.alexandrepiveteau.echo.core.buffer.mutableEventIdentifierGapBufferOf
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.binarySearchBySite
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as I
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as O
import kotlinx.coroutines.selects.select

/**
 * Receives all the advertising events from the other side, and awaits the first [I.Ready] message
 * before returning a [MutableEventIdentifierGapBuffer] containing all the available sites and
 * sequence numbers.
 */
internal suspend fun ExchangeScope<I, O>.awaitAdvertisements(): MutableEventIdentifierGapBuffer {
  val available = mutableEventIdentifierGapBufferOf()
  while (true) {
    when (val msg = receiveCatching().getOrNull()) {
      is I.Ready -> return available
      is I.Advertisement -> available.push(EventIdentifier(msg.nextSeqno, msg.site))
      is I.Events -> error("Didn't expect events $msg")
      null -> terminate()
    }
  }
}

/**
 * Awaits the reception of all the events from the other side. Whenever a new advertisement is
 * received, an acknowledgement and a request will be sent, in order.
 *
 * @param advertisements the [MutableEventIdentifierGapBuffer] of the advertisements that have been
 * received from the other side.
 * @param stopAfterAdvertised a [Boolean] with value `true` if only the [advertised] events should
 * be sent.
 */
internal suspend fun ExchangeScope<I, O>.awaitEvents(
    advertisements: MutableEventIdentifierGapBuffer,
    stopAfterAdvertised: Boolean,
) {
  var requestedIndex = 0

  // The queue of all the messages that still have to be sent to the other side. Messages are sent
  // in a FIFO fashion, and should simply be added to the queue.
  val queue = ArrayDeque<O>(advertisements.size)

  // We are done receiving whenever we've received a message from the other side telling us that
  // no more events should be sent. Nevertheless, we should still make sure that all the events have
  // been properly merged into the local MutableEventLog before terminating, otherwise we may not
  // respect strictly the sync once semantics.
  var isDoneReceiving = false

  // Repeat until the channel is closed.
  while (!isDoneReceiving) {

    // If we are syncing in a one-shot fashion, terminate if we have already received all the events
    // that we were expecting in a session.
    if (stopAfterAdvertised) terminateIfReceivedAllEvents(advertisements)

    // First, make sure we have some pending messages in the queue for all the sites that have been
    // advertised by the other side.
    if (requestedIndex < advertisements.size) {
      val acknowledgements = withEventLogLock { acknowledged() }

      // The advertisements array has already issued some Acknowledge and Request messages for the
      // identifiers present in the first requestedIndex cells of the advertisements array. When
      // some new acknowledgements are present in the local log, they will be advertised.
      while (requestedIndex < advertisements.size) {
        val currentSite = advertisements[requestedIndex].site

        val index = acknowledgements.binarySearchBySite(currentSite)
        val expected = if (index >= 0) acknowledgements[index].seqno.inc() else SequenceNumber.Min

        queue += O.Acknowledge(currentSite, expected)
        queue += O.Request(currentSite, UInt.MAX_VALUE)
        requestedIndex++
      }
    }

    // Next, handle sending and reception of the messages with the other side.
    select<Unit> {

      // Send the first message from the queue.
      val firstMsg = queue.firstOrNull()
      if (firstMsg != null) {
        onSend(firstMsg) { queue.removeFirst() }
      }

      // Receive a message from the other side.
      if (!isDoneReceiving) {
        onReceiveCatching { v ->
          when (val msg = v.getOrNull()) {
            is I.Advertisement -> {
              if (!stopAfterAdvertised)
                  advertisements.push(EventIdentifier(msg.nextSeqno, msg.site))
            }
            is I.Events ->
                withEventLogLock { msg.events.forEach { insert(it.seqno, it.site, it.data) } }
            is I.Ready -> error("Unexpected duplicate Ready.")
            null -> isDoneReceiving = true
          }
        }
      }
    }
  }
}

/**
 * Terminates if all the events in the [advertisements] buffer have already been received in the
 * local log. Otherwise, this function will simply return without any side effect.
 */
private suspend fun ExchangeScope<I, O>.terminateIfReceivedAllEvents(
    advertisements: MutableEventIdentifierGapBuffer,
) {
  // This could run in O(n).
  val acknowledged = withEventLogLock { acknowledged() }
  for (i in 0 until advertisements.size) {
    val expected = advertisements[i]
    val index = acknowledged.binarySearchBySite(expected.site)
    if (index < 0) return
    if (acknowledged[index].seqno.inc() < expected.seqno) return
  }
  terminate()
}
