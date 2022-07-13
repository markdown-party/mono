@file:OptIn(InternalCoroutinesApi::class)

package io.github.alexandrepiveteau.echo.webrtc.client.peers

import io.github.alexandrepiveteau.echo.webrtc.signaling.IceCandidate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult.Companion.closed
import kotlinx.coroutines.channels.ChannelResult.Companion.failure
import kotlinx.coroutines.channels.ChannelResult.Companion.success
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.js.jso
import webrtc.RTCIceCandidateInit
import webrtc.RTCPeerConnection

/**
 * An implementation of [ICEPeer] which makes use of an [RTCPeerConnection] under-the-hood to
 * establish communication with a remote peer.
 */
internal abstract class RTCPeerConnectionPeer : ICEPeer {

  /** The [Channel] representing the messages received though this [RTCPeerConnectionPeer]. */
  internal val incoming = Channel<String>(Channel.UNLIMITED)

  /** The [Channel] representing the messages sent through this [RTCPeerConnectionPeer] */
  internal val outgoing = Channel<String>(Channel.UNLIMITED)

  /** The [RTCPeerConnection] which allows communicating with the remote peer. */
  internal val connection = RTCPeerConnection(jso { iceServers = GoogleIceServers })

  override suspend fun receiveCatching() = incoming.receiveCatching()

  override suspend fun sendCatching(message: String) =
      try {
        success(outgoing.send(message))
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (closed: ClosedSendChannelException) {
        closed(closed.cause)
      } catch (throwable: Throwable) {
        failure()
      }

  override suspend fun ice(
      candidate: IceCandidate,
  ) = connection.addIceCandidate(JSON.parse<RTCIceCandidateInit>(candidate.json)).await()

  override fun cancel() {
    incoming.cancel()
    outgoing.cancel()
    connection.close()
  }
}
