package io.github.alexandrepiveteau.echo.webrtc.client

import io.github.alexandrepiveteau.echo.DefaultBinaryFormat
import io.github.alexandrepiveteau.echo.SendExchange
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.github.alexandrepiveteau.echo.webrtc.client.internal.BufferedWebSocketSession as WsSession
import io.github.alexandrepiveteau.echo.webrtc.client.internal.bufferedWs
import io.github.alexandrepiveteau.echo.webrtc.client.internal.bufferedWss
import io.github.alexandrepiveteau.echo.webrtc.client.peers.Callee
import io.github.alexandrepiveteau.echo.webrtc.client.peers.Caller
import io.github.alexandrepiveteau.echo.webrtc.signaling.ClientToClientMessage
import io.github.alexandrepiveteau.echo.webrtc.signaling.ClientToClientMessage.*
import io.github.alexandrepiveteau.echo.webrtc.signaling.PeerIdentifier
import io.github.alexandrepiveteau.echo.webrtc.signaling.SessionDescription
import io.github.alexandrepiveteau.echo.webrtc.signaling.SignalingMessage
import io.github.alexandrepiveteau.echo.webrtc.signaling.SignalingMessage.ClientToServer.Forward
import io.github.alexandrepiveteau.echo.webrtc.signaling.SignalingMessage.ServerToClient.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.whileSelect
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

/**
 * Invokes the given [block] with a [SignalingServer] available at the provided [request].
 *
 * @param exchange the [SendExchange] used to answer requests from the other side.
 * @param request the HTTP request builder.
 * @param block the block executed once the connection is established.
 */
suspend fun HttpClient.wsSignalingServer(
    exchange: SendExchange<Incoming, Outgoing>,
    request: HttpRequestBuilder.() -> Unit,
    block: suspend SignalingServer.() -> Unit,
): Unit =
    socketSignalingServer(
        exchange = exchange,
        factory = { bufferedWs(request) { it() } },
        block = block,
    )

/**
 * Invokes the given [block] with a [SignalingServer] available at the provided [request].
 *
 * @param exchange the [SendExchange] used to answer requests from the other side.
 * @param request the HTTP request builder.
 * @param block the block executed once the connection is established.
 */
suspend fun HttpClient.wssSignalingServer(
    exchange: SendExchange<Incoming, Outgoing>,
    request: HttpRequestBuilder.() -> Unit,
    block: suspend SignalingServer.() -> Unit,
): Unit =
    socketSignalingServer(
        exchange = exchange,
        factory = { bufferedWss(request) { it() } },
        block = block,
    )

/** A type alias representing a factory to create a socket. */
private typealias SocketFactory = suspend HttpClient.(suspend WsSession.() -> Unit) -> Unit

/**
 * Invokes the given [block] with a [SignalingServer] which was created through the provided
 * [SocketFactory].
 *
 * @param exchange the [SendExchange] used to answer requests from the other side.
 * @param factory the factory to create a websockets which is used with the [SignalingServer].
 * @param block the block to be invoked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun HttpClient.socketSignalingServer(
    exchange: SendExchange<Incoming, Outgoing>,
    factory: SocketFactory,
    block: suspend SignalingServer.() -> Unit,
) = factory {
  val callers = MutableStateFlow(emptySet<Caller>())
  val callees = MutableStateFlow(emptySet<Callee>())

  launch { block(SignalingServerImpl(callers)) }

  whileSelect {

    // Try to receive messages from the remote server.
    incoming.onReceive { frame ->
      when (val msg = frame.toServerToClient()) {
        is PeerJoined ->
            callers.value =
                callers.value +
                    msg.peer.callerIn(this@factory) {
                      outgoing.trySend(Forward(msg.peer, it).toFrame())
                    }
        is PeerLeft -> {
          // Cancel peer connections for callers (and callees).
          callers.value = callers.value - msg.peer
          callees.value = callees.value - msg.peer
        }
        is GotMessage ->
            when (val fwd = msg.message) {
              is Offer ->
                  callees.value =
                      callees.value +
                          msg.from.calleeIn(this@factory, fwd.offer, exchange) {
                            outgoing.trySend(Forward(msg.from, it).toFrame())
                          }
              is IceCaller -> callers.value[msg.from]?.ice(fwd.ice)
              is Answer -> callers.value[msg.from]?.answer(fwd.answer)
              is IceCallee -> callees.value[msg.from]?.ice(fwd.ice)
            }
      }

      // Keep looping.
      true
    }
  }
}

/**
 * Creates a new [Caller] for the given [PeerIdentifier].
 *
 * @param scope the [CoroutineScope] which determines the lifecycle of the [Caller].
 * @param message a way for the [Caller] to send messages to the callee.
 * @return the [Caller] instance.
 */
private fun PeerIdentifier.callerIn(
    scope: CoroutineScope,
    message: (ClientToClientMessage) -> Unit,
): Caller = Caller(scope, this, message)

/**
 * Creates a new [Callee] for the given [PeerIdentifier].
 *
 * @param scope the [CoroutineScope] which determines the lifecycle of the [Callee].
 * @param offer the [SessionDescription] which this callee answers to.
 * @param exchange the [SendExchange] that this callee should sync with.
 * @param message a way for the [Caller] to send messages to the callee.
 * @return the [Callee] instance.
 */
private fun PeerIdentifier.calleeIn(
    scope: CoroutineScope,
    offer: SessionDescription,
    exchange: SendExchange<Incoming, Outgoing>,
    message: (ClientToClientMessage) -> Unit,
): Callee = Callee(scope, offer, this, exchange, message)

/**
 * Finds the [Peer] with the given [PeerIdentifier], if any. If multiple elements match, the first
 * one will be returned.
 *
 * @param P the type of the [Peer].
 * @receiver the [Set] of the [Peer]s.
 * @param id the [PeerIdentifier] that we're looking for.
 * @return the resulting [Set].
 */
private operator fun <P : Peer> Set<P>.get(id: PeerIdentifier): P? = find { it.identifier == id }

/**
 * Removes the [Peer]s with the given [PeerIdentifier] from the receiver [Set].
 *
 * @param P the type of the [Peer].
 * @receiver the [Set] of [Peer]s.
 * @param id the [PeerIdentifier] that we're looking for.
 * @return the resulting [Set].
 */
private operator fun <P : Peer> Set<P>.minus(id: PeerIdentifier): Set<P> {
  val (t, f) = partition { it.identifier == id }
  t.forEach { it.cancel() }
  return f.toSet()
}

/** Decodes the [Frame] and returns the corresponding [SignalingMessage.ServerToClient]. */
private fun Frame.toServerToClient(): SignalingMessage.ServerToClient =
    DefaultBinaryFormat.decodeFromByteArray((this as Frame.Binary).data)

/** Encodes the [SignalingMessage.ClientToServer] and returns the corresponding [Frame]. */
private fun SignalingMessage.ClientToServer.toFrame(): Frame.Binary =
    Frame.Binary(fin = true, data = DefaultBinaryFormat.encodeToByteArray(this))

/**
 * An implementation of [SignalingServerImpl], which delegates its properties.
 *
 * @property peers the [SharedFlow] of peers for the server.
 */
private class SignalingServerImpl(override val peers: SharedFlow<Set<Peer>>) : SignalingServer
