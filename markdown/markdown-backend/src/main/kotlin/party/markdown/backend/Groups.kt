package party.markdown.backend

import io.github.alexandrepiveteau.echo.DefaultSerializationFormat
import io.ktor.websocket.*
import io.ktor.websocket.Frame.*
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToByteArray
import party.markdown.coroutines.inOrder
import party.markdown.signaling.PeerIdentifier
import party.markdown.signaling.SignalingMessage.ServerToClient

/**
 * A map of all the collaboration sessions which are currently underway. Each [Group] can be
 * accessed using a unique identifier, and it is guaranteed that a single [Group] will be created
 * for all the clients which request access using the same session identifier.
 *
 * @param scope the [CoroutineScope] in which the [GroupMap] is running.
 */
class GroupMap(private val scope: CoroutineScope) {

  /** The [Mutex] that protects the [groups]. */
  private val mutex = Mutex()

  /** The [Group]s, keyed by session identifier. */
  private val groups = mutableMapOf<SessionIdentifier, Group>()

  /**
   * Returns the [Group] associated to the given [SessionIdentifier].
   *
   * @param session the [SessionIdentifier] that uniquely identifies this group.
   * @return the [Group] for this collaboration session.
   */
  suspend fun get(
      session: SessionIdentifier,
  ): Group = mutex.withLock { groups.getOrPut(session) { Group(scope) } }
}

/**
 * An alternative to [SendChannel] which can be used to send some messages to a specific client and
 * catches [ClosedSendChannelException], but propagates cancellation exceptions properly.
 *
 * @param T the type of the messages sent.
 */
fun interface Outbox<in T> {

  /**
   * Send a message through this [Outbox].
   *
   * @param element the type of the sent element.
   */
  suspend fun sendCatching(element: T)

  companion object {

    /**
     * Creates an [Outbox] from the provided [SendChannel].
     *
     * @param T the type of the elements which can be sent.
     * @param channel the underlying [SendChannel].
     * @return the resulting [Outbox].
     */
    fun <T> wrap(channel: SendChannel<T>) =
        Outbox<T> { element ->
          try {
            channel.send(element)
          } catch (_: ClosedSendChannelException) {
            // Ignore, but propagate cancellation exceptions.
          }
        }
  }
}

/** Maps the given [Outbox] using the provided function. */
fun <A, B> Outbox<A>.map(
    f: suspend (B) -> A,
) = Outbox<B> { element -> this@map.sendCatching(f(element)) }

/**
 * A group represents a session of collaboration. Each participant will be granted a unique
 * identifier, and will be notified of new members.
 *
 * @param scope the [CoroutineScope] in which the [Group] is running.
 */
class Group(private val scope: CoroutineScope) {

  /** The `InOrder` used to schedule all the computations. */
  private val inOrder = scope.inOrder()

  /** The [MutableMap] of all the peer identifiers, and the [Outbox] for their messages. */
  private var peers = persistentMapOf<PeerIdentifier, Outbox<ServerToClient>>()

  /** Returns the next [PeerIdentifier] that should be attributed. */
  private fun nextPeerIdentifier() = PeerIdentifier(peers.keys.maxOfOrNull { it.id }?.plus(1) ?: 0)

  /**
   * Joins the [Group], and gets a new peer identifier assigned.
   *
   * @param outbox the [Outbox] for this participant.
   * @return the [PeerIdentifier] assigned to this participant.
   */
  private suspend fun join(outbox: Outbox<ServerToClient>): PeerIdentifier {
    return inOrder.schedule {
      val current = peers
      val peer = nextPeerIdentifier()
      peers = current.put(peer, outbox)
      withResult(peer) {
        for ((id, site) in current) {
          site.sendCatching(ServerToClient.PeerJoined(peer)) // Tell other sites we've joined.
          outbox.sendCatching(ServerToClient.PeerJoined(id)) // Learn all the connected sites.
        }
      }
    }
  }

  /**
   * Leaves the [Group], notifying the other members.
   *
   * @param peer the identifier of the peer who is leaving the group.
   */
  private suspend fun leave(peer: PeerIdentifier) {
    inOrder.schedule {
      val withoutPeer = peers.remove(peer)
      peers = withoutPeer
      withNoResult {
        for ((_, outbox) in withoutPeer) {
          outbox.sendCatching(ServerToClient.PeerLeft(peer))
        }
      }
    }
  }

  /**
   * Forwards a [message] to the given [peer].
   *
   * @param peer the [PeerIdentifier] to which the message is sent.
   * @param message the sent message.
   */
  private suspend fun forward(peer: PeerIdentifier, message: ServerToClient) {
    inOrder.schedule {
      withNoResult {
        val outbox = mutex.withLock { peers[peer] }
        outbox?.sendCatching(message)
      }
    }
  }

  /**
   * Starts a session within this [Group], ensuring that the user properly joins and then leaves the
   * group.
   *
   * @param outbox the [Outbox] for this participant.
   * @param block the [GroupScope] in which the participant may forward some messages.
   */
  suspend fun session(
      outbox: Outbox<ServerToClient>,
      block: suspend GroupScope.(PeerIdentifier) -> Unit,
  ) {
    val peer = join(outbox)
    try {
      block(GroupScope(this::forward), peer)
    } finally {
      withContext(NonCancellable) { leave(peer) }
    }
  }
}

/**
 * Starts a session within this [Group], ensuring that the user properly joins and then leaves the
 * group.
 *
 * @receiver the [Group] in which the session takes place.
 * @param session the [WebSocketSession] used for communication.
 * @param block the [GroupScope] in which the participant may forward some messages.
 */
suspend fun Group.session(
    session: WebSocketSession,
    block: suspend GroupScope.(PeerIdentifier) -> Unit,
) =
    session(
        Outbox.wrap(session.outgoing).map {
          Binary(true, DefaultSerializationFormat.encodeToByteArray(it))
        },
        block,
    )

/** An interface representing the operations which are available when in a session in a [Group]. */
fun interface GroupScope {

  /**
   * Forwards a [message] to the given [peer].
   *
   * @param peer the [PeerIdentifier] to which the message should be forwarded.
   * @param message the [ServerToClient] message to forward.
   */
  suspend fun forward(peer: PeerIdentifier, message: ServerToClient)
}
