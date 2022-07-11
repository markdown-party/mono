package party.markdown.p2p

import io.github.alexandrepiveteau.echo.DefaultSerializationFormat
import io.github.alexandrepiveteau.echo.SendExchange
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.plus
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
import party.markdown.p2p.ktor.BufferedWebSocketSession as WsSession
import party.markdown.p2p.ktor.bufferedWs
import party.markdown.p2p.ktor.bufferedWss
import party.markdown.p2p.wrappers.*
import party.markdown.signaling.PeerIdentifier
import party.markdown.signaling.SignalingMessage.ClientToServer
import party.markdown.signaling.SignalingMessage.ServerToClient
import webrtc.RTCIceServer
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
) = factory { block(WsSessionSignalingServer(exchange, this)) }

private class WsSessionSignalingServer(
    exchange: SendExchange<Incoming, Outgoing>,
    session: WsSession,
) : WsSession by session, SignalingServer, Comm {

  private val state = State(this, exchange)

  init {
    launch {
      for (frame in incoming) {
        val bytes = (frame as Frame.Binary).readBytes()
        when (val msg = DefaultSerializationFormat.decodeFromByteArray<ServerToClient>(bytes)) {
          is ServerToClient.GotMessage -> {
            val from = msg.from
            when (val message = DefaultStringFormat.decodeFromString<Message>(msg.message)) {
              is Message.Answer -> state.handleAnswer(from, message.channel, message.answer)
              is Message.Offer -> state.handleOffer(from, message.channel, message.offer)
              is Message.IceCaller -> state.handleIceCaller(from, message.channel, message.ice)
              is Message.IceCallee -> state.handleIceCallee(from, message.channel, message.ice)
            }
          }
          is ServerToClient.PeerJoined -> state.addPeer(msg.peer)
          is ServerToClient.PeerLeft -> state.removePeer(msg.peer)
        }
      }
    }
  }

  override val peers = state.peers

  override fun enqueue(to: PeerIdentifier, message: Message) {
    val encoded = DefaultStringFormat.encodeToString(message)
    // KLUDGE : We need to explicitly mark this as ClientToServer or Kotlinx serialization may not
    //          work properly.
    val forward: ClientToServer = ClientToServer.Forward(to = to, message = encoded)
    val frame = Frame.Binary(true, DefaultSerializationFormat.encodeToByteArray(forward))
    outgoing.trySend(frame)
  }

  override suspend fun connect(peer: PeerIdentifier): PeerToPeerConnection {
    val caller = state.create(peer)
    return object : PeerToPeerConnection {
      override val incoming: ReceiveChannel<String> = caller.incoming
      override val outgoing: SendChannel<String> = caller.outgoing
    }
  }
}

private val GoogleIceServers =
    arrayOf<RTCIceServer>(
        jso { urls = "stun:stun.l.google.com:19302" },
    )

// TODO : Clean this up afterwards.

interface Comm {
  fun enqueue(to: PeerIdentifier, message: Message)
}

data class PeerChannelId(val peer: PeerIdentifier, val channel: ChannelId)

class State(
    private val comm: Comm,
    private val exchange: SendExchange<Incoming, Outgoing>,
) : Comm by comm {

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
  private var members = persistentSetOf<PeerIdentifier>()
  private val callers = mutableMapOf<PeerChannelId, Caller>()
  private val callees = mutableMapOf<PeerChannelId, Callee>()

  private val membersFlow =
      MutableSharedFlow<Set<PeerIdentifier>>(
              replay = 1,
              extraBufferCapacity = Int.MAX_VALUE,
          )
          .apply { tryEmit(emptySet()) }

  val peers: SharedFlow<Set<PeerIdentifier>> = membersFlow.asSharedFlow()

  fun addPeer(peer: PeerIdentifier) {
    members += peer
    membersFlow.tryEmit(members)
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
    members -= peer
    membersFlow.tryEmit(members)
  }

  suspend fun create(peer: PeerIdentifier): Caller {
    val channelId = nextChannelId()
    val caller = Caller().also { callers[PeerChannelId(peer, channelId)] = it }

    with(caller.connection) {
      // Send all the Ice candidates.
      caller.forward(createDataChannel(null), caller.incoming, caller.outgoing)
      onicecandidate { event ->
        event.candidate?.let { comm.enqueue(peer, Message.IceCallee(channelId, it)) }
      }
      val offer = createOfferSuspend()
      setLocalDescriptionSuspend(offer)
      enqueue(peer, Message.Offer(channelId, offer))
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
      onicecandidate { event ->
        event.candidate?.let { comm.enqueue(from, Message.IceCaller(channel, it)) }
      }
      setRemoteDescriptionSuspend(offer)
      val answer = createAnswerSuspend()
      setLocalDescriptionSuspend(answer)
      enqueue(from, Message.Answer(channel, answer))
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
