package io.github.alexandrepiveteau.echo.webrtc.client

import kotlin.coroutines.resume
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import webrtc.*

/** The type of the JS events to query when fetching ICE information. */
private const val IceCandidateEventType = "icecandidate"

/**
 * Returns the available [RTCIceCandidateInit] from this [RTCPeerConnection] as a [Flow].
 *
 * @receiver the [RTCPeerConnection] for which the events are retrieved.
 * @return the [Flow] of [RTCIceCandidateInit] which are being generated.
 */
internal fun RTCPeerConnection.iceCandidatesAsFlow(): Flow<RTCIceCandidateInit> {
  return callbackFlow {
        val listener: (Event) -> Unit = {
          val event = it.unsafeCast<RTCPeerConnectionIceEvent>()
          val candidate = event.asDynamic().candidate.unsafeCast<RTCIceCandidateInit?>()
          if (candidate != null) trySend(candidate)
        }
        addEventListener(IceCandidateEventType, listener)
        awaitClose { removeEventListener(IceCandidateEventType, listener) }
      }
      .buffer(Channel.UNLIMITED)
}

/** The type of the JS events to query when fetching data channels. */
private const val DataChannelEventType = "datachannel"

/**
 * Returns the [RTCDataChannel] created by the other side of the connection, as soon as it's
 * available.
 *
 * @receiver the [RTCPeerConnection] for which the data channel is created.
 * @return the [RTCDataChannel] which is used.
 */
internal suspend fun RTCPeerConnection.awaitDataChannel(): RTCDataChannel {
  return suspendCancellableCoroutine { cont ->
    val listener =
        object : EventListener {
          override fun handleEvent(event: Event) {
            removeEventListener(DataChannelEventType, this)
            val rtcEvent = event.unsafeCast<RTCDataChannelEvent>()
            cont.resume(rtcEvent.channel)
          }
        }
    addEventListener(DataChannelEventType, listener)
    cont.invokeOnCancellation { removeEventListener(DataChannelEventType, listener) }
  }
}
