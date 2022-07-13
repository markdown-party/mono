package io.github.alexandrepiveteau.echo.webrtc.client.internal

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.w3c.dom.MessageEvent
import webrtc.RTCDataChannel

/** The type of "open" events. */
private const val EventTypeOpen = "open"

/** The type of "message" events. */
private const val EventTypeMessage = "message"

/**
 * Waits until the [RTCDataChannel] is open, and sends all the messages from [outgoing] while
 * receiving messages from the channel into [incoming].
 *
 * @receiver the [RTCDataChannel] through which messages are sent.
 * @param incoming the [SendChannel] to which events from the [RTCDataChannel] are sent.
 * @param outgoing the [ReceiveChannel] from which events are taken to be sent through the
 * [RTCDataChannel].
 */
internal suspend fun RTCDataChannel.handle(
    incoming: SendChannel<String>,
    outgoing: ReceiveChannel<String>
): Unit = coroutineScope {
  awaitEvent(EventTypeOpen)
  launch { for (message in outgoing) send(message) }
  eventFlow(EventTypeMessage)
      .filterIsInstance<MessageEvent>()
      .onEach { incoming.send(it.data as String) }
      .collect()
}
