package party.markdown.p2p

import io.github.alexandrepiveteau.echo.DefaultBinaryFormat
import io.github.alexandrepiveteau.echo.SendExchange
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.github.alexandrepiveteau.echo.webrtc.signaling.*
import io.github.alexandrepiveteau.echo.webrtc.signaling.ClientToClientMessage.*
import io.github.alexandrepiveteau.echo.webrtc.signaling.SignalingMessage.ClientToServer
import io.github.alexandrepiveteau.echo.webrtc.signaling.SignalingMessage.ServerToClient
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import io.ktor.websocket.Frame.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.js.jso
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import ktor.BufferedWebSocketSession as WsSession
import ktor.bufferedWs
import ktor.bufferedWss
import party.markdown.p2p.wrappers.*
import party.markdown.peerToPeer.PeerToPeerConnection
import party.markdown.peerToPeer.webRTC.GoogleIceServers
import webrtc.RTCPeerConnection

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
private suspend fun HttpClient.socketSignalingServer(
    exchange: SendExchange<Incoming, Outgoing>,
    factory: SocketFactory,
    block: suspend SignalingServer.() -> Unit,
) = factory {
  // TODO : This should be the caller's responsibility.
  while (true) {
    block(WsSessionSignalingServer(exchange, this))
    delay(RetryDelaySignalingServer)
  }
}

/**
 * Sends a message to the peer with the provided [PeerIdentifier] through the [WsSession].
 *
 * @receiver the [WsSession] in which the messages are sent.
 * @param to the identifier of the receiver peer.
 * @param message the [ClientToClientMessage] that is sent.
 */
private fun WsSession.send(to: PeerIdentifier, message: ClientToClientMessage) {
  val forward = ClientToServer.Forward(to = to, message = message)
  val frame = Binary(true, DefaultBinaryFormat.encodeToByteArray<ClientToServer>(forward))
  outgoing.trySend(frame)
}

/**
 * Runs the given [block] for each message that is received in this [WsSession].
 *
 * @receiver the [WsSession] on which the messages are read.
 * @param block the block that gets invoked on each message.
 */
private suspend inline fun WsSession.forEachServerToClient(block: (ServerToClient) -> Unit) {
  for (frame in incoming) {
    val bytes = (frame as Binary).readBytes()
    val msg = DefaultBinaryFormat.decodeFromByteArray<ServerToClient>(bytes)
    block(msg)
  }
}

private class WsSessionSignalingServer(
    private val exchange: SendExchange<Incoming, Outgoing>,
    session: WsSession,
) : WsSession by session, SignalingServer {

  init {
    launch {
      forEachServerToClient { msg ->
        when (msg) {
          is ServerToClient.GotMessage ->
              when (val message = msg.message) {
                is Answer -> handleAnswer(msg.from, message.channel, message.answer)
                is Offer -> handleOffer(msg.from, message.channel, message.offer)
                is IceCaller -> handleIceCaller(msg.from, message.channel, message.ice)
                is IceCallee -> handleIceCallee(msg.from, message.channel, message.ice)
              }
          is ServerToClient.PeerJoined -> addPeer(msg.peer)
          is ServerToClient.PeerLeft -> removePeer(msg.peer)
        }
      }
    }
  }

  override suspend fun connect(peer: PeerIdentifier): PeerToPeerConnection {
    val caller = create(peer)
    return object : PeerToPeerConnection {
      override val incoming: ReceiveChannel<String> = caller.incoming
      override val outgoing: SendChannel<String> = caller.outgoing
    }
  }

  private var _id = 0
  private suspend fun nextChannelId(): ChannelId = mutex.withLock { ChannelId(_id++) }

  // TODO : Caller and Callee are identical, merge them at some point.
  data class Caller(
      val incoming: Channel<String> = Channel(UNLIMITED),
      val outgoing: Channel<String> = Channel(UNLIMITED),
      val connection: RTCPeerConnection = RTCPeerConnection(jso { iceServers = GoogleIceServers }),
  ) : CoroutineScope by CoroutineScope(Job())

  data class Callee(
      val incoming: Channel<String> = Channel(UNLIMITED),
      val outgoing: Channel<String> = Channel(UNLIMITED),
      val connection: RTCPeerConnection = RTCPeerConnection(jso { iceServers = GoogleIceServers }),
  ) : CoroutineScope by CoroutineScope(Job())

  private val mutex = Mutex()
  private val callers = mutableMapOf<PeerChannelId, Caller>()
  private val callees = mutableMapOf<PeerChannelId, Callee>()

  override val peers = MutableStateFlow(emptySet<PeerIdentifier>())

  fun addPeer(peer: PeerIdentifier) {
    peers.update { it + peer }
  }

  fun removePeer(peer: PeerIdentifier) {
    // Clear the callers and callees associated with the removed peer.
    // TODO : Factorize this code.
    for (id in callers.keys.filter { it.peer == peer }) {
      val connection = callers.remove(id)
      connection?.incoming?.close()
      connection?.outgoing?.close()
      connection?.cancel()
    }
    for (id in callees.keys.filter { it.peer == peer }) {
      val connection = callees.remove(id)
      connection?.incoming?.close()
      connection?.outgoing?.close()
      connection?.cancel()
    }
    // Remove the peer from the members.
    peers.update { it - peer }
  }

  suspend fun create(peer: PeerIdentifier): Caller {
    val channelId = nextChannelId()
    val caller = Caller().also { callers[PeerChannelId(peer, channelId)] = it }

    with(caller.connection) {
      // Send all the Ice candidates.
      caller.forward(createDataChannel(null), caller.incoming, caller.outgoing)
      onicecandidate { event -> event.candidate?.let { send(peer, IceCallee(channelId, it)) } }
      val offer = createOfferSuspend()
      setLocalDescriptionSuspend(offer)
      send(peer, Offer(channelId, offer))
    }
    return caller
  }

  suspend fun handleOffer(
      from: PeerIdentifier,
      channel: ChannelId,
      offer: SessionDescription,
  ) {
    val callee = Callee().also { callees[PeerChannelId(from, channel)] = it }

    with(callee) {
      launch {
        exchange
            .send(incoming.consumeAsFlow().map { DefaultStringFormat.decodeFromString(it) })
            .onEach { outgoing.send(DefaultStringFormat.encodeToString(it)) }
            .onCompletion { outgoing.close() }
            .collect()
      }
    }

    with(callee.connection) {
      // Send all the Ice candidates.
      ondatachannel = {
        callee.forward(it.channel, callee.incoming, callee.outgoing)
        null
      }
      onicecandidate { event -> event.candidate?.let { send(from, IceCaller(channel, it)) } }
      setRemoteDescriptionSuspend(offer)
      val answer = createAnswerSuspend()
      setLocalDescriptionSuspend(answer)
      send(from, Answer(channel, answer))
    }
  }

  suspend fun handleAnswer(
      from: PeerIdentifier,
      channel: ChannelId,
      answerDescription: SessionDescription,
  ) {
    val caller = callers[PeerChannelId(from, channel)] ?: return
    caller.connection.setRemoteDescriptionSuspend(answerDescription)
  }

  suspend fun handleIceCaller(
      from: PeerIdentifier,
      channel: ChannelId,
      iceCandidate: IceCandidate,
  ) {
    val caller = callers[PeerChannelId(from, channel)] ?: return
    caller.connection.addIceCandidateSuspend(iceCandidate)
  }

  suspend fun handleIceCallee(
      from: PeerIdentifier,
      channel: ChannelId,
      iceCandidate: IceCandidate,
  ) {
    val callee = callees[PeerChannelId(from, channel)] ?: return
    callee.connection.addIceCandidateSuspend(iceCandidate)
  }
}

data class PeerChannelId(val peer: PeerIdentifier, val channel: ChannelId)
