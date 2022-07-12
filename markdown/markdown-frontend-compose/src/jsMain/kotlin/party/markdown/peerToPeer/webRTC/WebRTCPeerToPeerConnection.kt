package party.markdown.peerToPeer.webRTC

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.js.jso
import party.markdown.peerToPeer.PeerToPeerConnection
import io.github.alexandrepiveteau.echo.webrtc.signaling.IceCandidate
import io.github.alexandrepiveteau.echo.webrtc.signaling.SessionDescription
import webrtc.RTCDataChannel
import webrtc.RTCIceCandidateInit
import webrtc.RTCIceServer
import webrtc.RTCPeerConnection

/**
 * An implementation of a [PeerToPeerConnection] which internally uses a [RTCDataChannel] and
 * supports the WebRTC handshake protocol.
 *
 * @param scope the [CoroutineScope] in which this [PeerToPeerConnection] is running.
 */
class WebRTCPeerToPeerConnection
private constructor(
    private val connection: RTCPeerConnection,
    override val incoming: ReceiveChannel<String>,
    override val outgoing: SendChannel<String>,
) : PeerToPeerConnection {

  companion object Factory {

    fun empty(scope: CoroutineScope): WebRTCPeerToPeerConnection {
      val connection = RTCPeerConnection(jso { iceServers = GoogleIceServers })
      val channel = connection.createDataChannel(null)
      return WebRTCPeerToPeerConnection(
          connection = connection,
          incoming = channel.asReceiveChannel(),
          outgoing = channel.asSendChannelIn(scope),
      )
    }

    fun fromOffer(scope: CoroutineScope, offer: SessionDescription) {
      val connection = RTCPeerConnection(jso { iceServers = GoogleIceServers })
      connection.ondatachannel = {
        // TODO : how tf do I handle this ??
        null
      }
    }
  }

  /**
   * Sets the local [SessionDescription] for this [WebRTCPeerToPeerConnection], and awaits.
   *
   * @param description the [SessionDescription] which is set.
   */
  suspend fun setLocalDescriptionSuspend(description: SessionDescription): Unit =
      connection.setLocalDescription(JSON.parse(description.json)).await()

  /**
   * Sets the remote [SessionDescription] for this [WebRTCPeerToPeerConnection], and awaits.
   *
   * @param description the [SessionDescription] which is set.
   */
  suspend fun setRemoteDescriptionSuspend(description: SessionDescription): Unit =
      connection.setRemoteDescription(JSON.parse(description.json)).await()

  /** Returns the offer [SessionDescription] for this [WebRTCPeerToPeerConnection]. */
  suspend fun createOfferSuspend(): SessionDescription =
      SessionDescription(JSON.stringify(connection.createOffer().await()))

  /** Returns the answer [SessionDescription] for this [WebRTCPeerToPeerConnection]. */
  suspend fun createAnswerSuspend(): SessionDescription =
      SessionDescription(JSON.stringify(connection.createAnswer().await()))

  /**
   * Adds an [IceCandidate] for this [WebRTCPeerToPeerConnection], and awaits.
   *
   * @param candidate the [IceCandidate] which is added to the [RTCPeerConnection].
   */
  suspend fun addIceCandidateSuspend(candidate: IceCandidate): Unit =
      connection.addIceCandidate(JSON.parse<RTCIceCandidateInit>(candidate.json)).await()

  // TODO : Surface this in PeerToPeerConnection ?
  fun cancel() {
    incoming.cancel()
    outgoing.close()
  }
}

/** The [Array] of [RTCIceServer] which can be used for computing the ICE candidates. */
val GoogleIceServers = arrayOf<RTCIceServer>(jso { urls = "stun:stun.l.google.com:19302" })
