package io.github.alexandrepiveteau.echo.webrtc.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import webrtc.RTCDataChannel

internal fun CoroutineScope.forward(
    channel: RTCDataChannel,
    incoming: SendChannel<String>,
    outgoing: ReceiveChannel<String>,
) {
  // TODO : This looks kind of ugly. Are the edge cases correctly handled ??
  channel.onopen = {
    launch { for (msg in outgoing) channel.send(msg) }
    null
  }
  channel.onmessage = {
    incoming.trySend(it.data.unsafeCast<String>())
    null
  }
}
