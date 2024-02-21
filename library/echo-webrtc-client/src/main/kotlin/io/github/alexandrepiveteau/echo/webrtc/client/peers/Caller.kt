package io.github.alexandrepiveteau.echo.webrtc.client.peers

import io.github.alexandrepiveteau.echo.webrtc.client.internal.eventFlow
import io.github.alexandrepiveteau.echo.webrtc.client.internal.handle
import io.github.alexandrepiveteau.echo.webrtc.signaling.ClientToClientMessage
import io.github.alexandrepiveteau.echo.webrtc.signaling.IceCandidate
import io.github.alexandrepiveteau.echo.webrtc.signaling.PeerIdentifier
import io.github.alexandrepiveteau.echo.webrtc.signaling.SessionDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import webrtc.RTCIceCandidateInit
import webrtc.RTCPeerConnectionIceEvent

/**
 * An [RTCPeerConnectionPeer] which represents the caller side of a WebRTC connection.
 *
 * @param scope the [CoroutineScope] in which the peer is created.
 * @param identifier the [PeerIdentifier] for this peer.
 * @param message a way for the [Caller] to send messages to its [Callee].
 */
internal class Caller(
    scope: CoroutineScope,
    override val identifier: PeerIdentifier,
    message: (ClientToClientMessage) -> Unit,
) : RTCPeerConnectionPeer() {

  /** The `RTCDataChannel` through which data is sent. */
  private val channel = connection.createDataChannel(null)

  /** A job which sends an offer to the callee. */
  private val jobStart =
      scope.launch {
        val offer = connection.createOffer().await()
        connection.setLocalDescription(offer).await()
        val description = SessionDescription(JSON.stringify(offer))
        message(ClientToClientMessage.Offer(description))
      }

  /** A job which propagates ICE candidates to the callee. */
  private val jobIce =
      scope.launch {
        connection
            .eventFlow("icecandidate")
            .filterIsInstance<RTCPeerConnectionIceEvent>()
            .mapNotNull { it.asDynamic().candidate.unsafeCast<RTCIceCandidateInit?>() }
            .map(JSON::stringify)
            .onEach { message(ClientToClientMessage.IceCallee(IceCandidate(it))) }
            .collect()
      }

  /** A job which handles the data channel. */
  private val jobChannel = scope.launch { channel.handle(incoming, outgoing) }

  /**
   * Adds an answer [SessionDescription] to this peer.
   *
   * @param session the session description from the answer.
   */
  suspend fun answer(session: SessionDescription) {
    connection.setRemoteDescription(JSON.parse(session.json)).await()
  }

  override fun cancel() {
    super.cancel()
    jobStart.cancel()
    jobIce.cancel()
    jobChannel.cancel()
  }
}
