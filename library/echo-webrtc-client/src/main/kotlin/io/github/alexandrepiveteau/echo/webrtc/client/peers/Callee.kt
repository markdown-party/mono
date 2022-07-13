@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")

package io.github.alexandrepiveteau.echo.webrtc.client.peers

import io.github.alexandrepiveteau.echo.SendExchange
import io.github.alexandrepiveteau.echo.protocol.Message
import io.github.alexandrepiveteau.echo.webrtc.client.internal.awaitEvent
import io.github.alexandrepiveteau.echo.webrtc.client.internal.encode
import io.github.alexandrepiveteau.echo.webrtc.client.internal.eventFlow
import io.github.alexandrepiveteau.echo.webrtc.client.internal.handle
import io.github.alexandrepiveteau.echo.webrtc.client.sync
import io.github.alexandrepiveteau.echo.webrtc.signaling.ClientToClientMessage
import io.github.alexandrepiveteau.echo.webrtc.signaling.IceCandidate
import io.github.alexandrepiveteau.echo.webrtc.signaling.PeerIdentifier
import io.github.alexandrepiveteau.echo.webrtc.signaling.SessionDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import webrtc.RTCDataChannelEvent
import webrtc.RTCIceCandidateInit
import webrtc.RTCPeerConnectionIceEvent

/** The type of "datachannel" events. */
private const val EventTypeDataChannel = "datachannel"

/**
 * An [RTCPeerConnectionPeer] which represents the callee side of a WebRTC connection.
 *
 * @param scope the [CoroutineScope] in which the peer is created.
 * @param offer the [SessionDescription] with which the callee is created.
 * @param identifier the [PeerIdentifier] for this peer.
 * @param exchange the [SendExchange] to which the peer should sync.
 * @param message a way for the [Callee] to send messages to its [Caller].
 */
internal class Callee(
    scope: CoroutineScope,
    offer: SessionDescription,
    override val identifier: PeerIdentifier,
    exchange: SendExchange<Message.Incoming, Message.Outgoing>,
    message: (ClientToClientMessage) -> Unit,
) : RTCPeerConnectionPeer() {

  /** A job which syncs this peer with the exchange. */
  private val jobSync = scope.launch { exchange.encode().sync(this@Callee) }

  /** A job which sends an answer for the caller's offer. */
  private val jobStart =
      scope.launch {
        connection.setRemoteDescription(JSON.parse(offer.json)).await()
        val answer = connection.createAnswer().await()
        connection.setLocalDescription(answer).await()
        val description = SessionDescription(JSON.stringify(answer))
        message(ClientToClientMessage.Answer(description))
      }

  /** A job which propagates ICE candidates to the caller. */
  private val jobIce =
      scope.launch {
        connection
            .eventFlow("icecandidate")
            .filterIsInstance<RTCPeerConnectionIceEvent>()
            .mapNotNull { it.asDynamic().candidate.unsafeCast<RTCIceCandidateInit?>() }
            .map(JSON::stringify)
            .onEach { message(ClientToClientMessage.IceCaller(IceCandidate(it))) }
            .collect()
      }

  /** A job which handles the data channel. */
  private val jobChannel =
      scope.launch {
        val event = connection.awaitEvent(EventTypeDataChannel) as RTCDataChannelEvent
        event.channel.handle(incoming, outgoing)
      }

  override fun cancel() {
    super.cancel()
    jobStart.cancel()
    jobIce.cancel()
    jobSync.cancel()
    jobChannel.cancel()
  }
}
