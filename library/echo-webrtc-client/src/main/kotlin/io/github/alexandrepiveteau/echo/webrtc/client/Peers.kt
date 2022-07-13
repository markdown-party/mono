@file:OptIn(InternalCoroutinesApi::class)
@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")

package io.github.alexandrepiveteau.echo.webrtc.client

import io.github.alexandrepiveteau.echo.SendExchange
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.github.alexandrepiveteau.echo.webrtc.client.internal.awaitEvent
import io.github.alexandrepiveteau.echo.webrtc.client.internal.encode
import io.github.alexandrepiveteau.echo.webrtc.client.internal.eventFlow
import io.github.alexandrepiveteau.echo.webrtc.client.peers.GoogleIceServers
import io.github.alexandrepiveteau.echo.webrtc.signaling.*
import io.github.alexandrepiveteau.echo.webrtc.signaling.ClientToClientMessage.Offer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.channels.ChannelResult.Companion.closed
import kotlinx.coroutines.channels.ChannelResult.Companion.failure
import kotlinx.coroutines.channels.ChannelResult.Companion.success
import kotlinx.coroutines.flow.*
import kotlinx.js.jso
import org.w3c.dom.MessageEvent
import webrtc.*

internal interface ICEPeer : Peer {
  suspend fun ice(candidate: IceCandidate)
}

internal abstract class RTCPeer : ICEPeer {

  internal val incoming = Channel<String>(Channel.UNLIMITED)
  internal val outgoing = Channel<String>(Channel.UNLIMITED)

  internal val connection = RTCPeerConnection(jso { iceServers = GoogleIceServers })

  override suspend fun receiveCatching(): ChannelResult<String> {
    return incoming.receiveCatching()
  }

  override suspend fun sendCatching(message: String): ChannelResult<Unit> {
    return try {
      success(outgoing.send(message))
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (closed: ClosedSendChannelException) {
      closed(closed.cause)
    } catch (throwable: Throwable) {
      failure()
    }
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

internal class Caller(
    scope: CoroutineScope,
    override val identifier: PeerIdentifier,
    message: (ClientToClientMessage) -> Unit,
) : RTCPeer() {

  private val channel = connection.createDataChannel(null)

  private val jobStart =
      scope.launch {
        val offer = connection.createOffer().await()
        connection.setLocalDescription(offer).await()
        val description = SessionDescription(JSON.stringify(offer))
        message(Offer(ChannelId(0), description))
      }

  private val jobIce =
      scope.launch {
        connection
            .eventFlow("icecandidate")
            .filterIsInstance<RTCPeerConnectionIceEvent>()
            .mapNotNull { it.asDynamic().candidate.unsafeCast<RTCIceCandidateInit?>() }
            .map(JSON::stringify)
            .onEach { message(ClientToClientMessage.IceCallee(ChannelId(0), IceCandidate(it))) }
            .collect()
      }

  private val jobChannel = scope.launch { channel.handle(incoming, outgoing) }

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

internal class Callee(
    scope: CoroutineScope,
    offer: SessionDescription,
    override val identifier: PeerIdentifier,
    exchange: SendExchange<Incoming, Outgoing>,
    message: (ClientToClientMessage) -> Unit,
) : RTCPeer() {

  private val jobSync = scope.launch { exchange.encode().sync(this@Callee) }

  private val jobStart =
      scope.launch {
        connection.setRemoteDescription(JSON.parse(offer.json)).await()
        val answer = connection.createAnswer().await()
        connection.setLocalDescription(answer).await()
        val description = SessionDescription(JSON.stringify(answer))
        message(ClientToClientMessage.Answer(ChannelId(0), description))
      }

  private val jobIce =
      scope.launch {
        connection
            .eventFlow("icecandidate")
            .filterIsInstance<RTCPeerConnectionIceEvent>()
            .mapNotNull { it.asDynamic().candidate.unsafeCast<RTCIceCandidateInit?>() }
            .map(JSON::stringify)
            .onEach { message(ClientToClientMessage.IceCaller(ChannelId(0), IceCandidate(it))) }
            .collect()
      }

  private val jobChannel =
      scope.launch {
        val event = connection.awaitEvent("datachannel") as RTCDataChannelEvent
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

private suspend fun RTCDataChannel.handle(
    incoming: SendChannel<String>,
    outgoing: ReceiveChannel<String>
) = coroutineScope {
  awaitEvent("open")
  launch { for (message in outgoing) send(message) }
  eventFlow("message")
      .filterIsInstance<MessageEvent>()
      .onEach { incoming.send(it.data as String) }
      .collect()
}
