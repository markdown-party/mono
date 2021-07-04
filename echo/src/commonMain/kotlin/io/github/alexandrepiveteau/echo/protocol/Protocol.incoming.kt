package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.core.buffer.MutableEventIdentifierGapBuffer
import io.github.alexandrepiveteau.echo.core.buffer.mutableEventIdentifierGapBufferOf
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.binarySearchBySite
import io.github.alexandrepiveteau.echo.core.log.isNotEmpty
import io.github.alexandrepiveteau.echo.core.log.mutableEventLogOf
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as I
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as O
import kotlinx.coroutines.selects.select

/**
 * Starts the incoming side of the protocol, receiving advertisements, emitting requests and
 * receiving events as they are made available by the other site.
 */
internal suspend fun ExchangeScope<I, O>.startIncoming() = runCatchingTermination {
  val advertisements = awaitAdvertisements()
  awaitEvents(advertisements)
}

/**
 * Receives all the advertising events from the other side, and awaits the first [I.Ready] message
 * before returning a [MutableEventIdentifierGapBuffer] containing all the available sites and
 * sequence numbers.
 */
private suspend fun ExchangeScope<I, O>.awaitAdvertisements(): MutableEventIdentifierGapBuffer {
  val available = mutableEventIdentifierGapBufferOf()
  while (true) {
    when (val msg = receiveCatching().getOrNull()) {
      is I.Ready -> return available
      is I.Advertisement -> available.push(EventIdentifier(msg.nextSeqno, msg.site))
      is I.Event -> error("Didn't expect event $msg")
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
 */
private suspend fun ExchangeScope<I, O>.awaitEvents(
    advertisements: MutableEventIdentifierGapBuffer,
) {
  var requestedIndex = 0

  // The queue of all the messages that still have to be sent to the other side. Messages are sent
  // in a FIFO fashion, and should simply be added to the queue.
  val queue = ArrayDeque<O>(advertisements.size)
  val events = mutableEventLogOf()

  // Repeat until the channel is closed.
  while (true) {

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
      onReceiveCatching { v ->
        when (val msg = v.getOrNull()) {
          is I.Advertisement -> advertisements.push(EventIdentifier(msg.nextSeqno, msg.site))
          is I.Event -> events.insert(msg.seqno, msg.site, msg.body)
          is I.Ready -> error("Unexpected duplicate Ready.")
          null -> terminate()
        }
      }

      // Insert batches of events.
      if (events.isNotEmpty()) {
        onMutableEventLogLock { log ->
          log.merge(events)
          events.clear()
        }
      }
    }
  }
}
