package io.github.alexandrepiveteau.markdown.backend

import io.github.alexandrepiveteau.echo.DefaultSerializationFormat
import io.ktor.websocket.*
import io.ktor.websocket.Frame.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToByteArray
import party.markdown.signaling.PeerIdentifier
import party.markdown.signaling.SignalingMessage.ServerToClient

/**
 * A map of all the collaboration sessions which are currently underway. Each [Group] can be
 * accessed using a unique identifier, and it is guaranteed that a single [Group] will be created
 * for all the clients which request access using the same session identifier.
 */
class GroupMap {

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
  ): Group = mutex.withLock { groups.getOrPut(session) { Group() } }
}

/**
 * An alternative to [SendChannel] which can be used to send some messages to a specific client and
 * catches [ClosedSendChannelException], but propagates cancellation exceptions properly.
 *
 * @param T the type of the messages sent.
 */
private fun interface Outbox<in T> {

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
private fun <A, B> Outbox<A>.map(
    f: suspend (B) -> A,
) = Outbox<B> { element -> this@map.sendCatching(f(element)) }

/**
 * A group represents a session of collaboration. Each participant will be granted a unique
 * identifier, and will be notified of new members.
 */
class Group : GroupScope {

  /** The [Mutex] that protects the group. */
  private val mutex = Mutex()

  /** The [MutableMap] of all the peer identifiers, and the [Outbox] for their messages. */
  private val peers = mutableMapOf<PeerIdentifier, Outbox<ServerToClient>>()

  /**
   * Joins the [Group], and gets a new peer identifier assigned.
   *
   * @param session the [WebSocketSession] used for joining.
   * @return the [PeerIdentifier] assigned to this participant.
   */
  private suspend fun join(session: WebSocketSession): PeerIdentifier {
    val (peer, sites) =
        mutex.withLock {
          val id = peers.keys.maxOfOrNull { it.id }?.plus(1) ?: 0
          val peer = PeerIdentifier(id)
          peers[peer] =
              Outbox.wrap(session.outgoing).map {
                Binary(true, DefaultSerializationFormat.encodeToByteArray(it))
              }
          peer to peers
        }
    for ((id, site) in sites) {
      if (id != peer) site.sendCatching(ServerToClient.PeerJoined(peer))
    }
    return peer
  }

  /**
   * Leaves the [Group], notifying the other members.
   *
   * @param peer the identifier of the peer who is leaving the group.
   */
  private suspend fun leave(peer: PeerIdentifier) {
    val peers =
        mutex.withLock {
          peers -= peer
          peers.toMap()
        }
    for ((id, outbox) in peers) {
      if (id != peer) outbox.sendCatching(ServerToClient.PeerLeft(peer))
    }
  }

  override suspend fun forward(peer: PeerIdentifier, message: ServerToClient) {
    val channel = mutex.withLock { peers[peer] } ?: return
    channel.sendCatching(message)
  }

  /**
   * Starts a session within this [Group], ensuring that the user properly joins and then leaves the
   * group.
   *
   * @receiver the [WebSocketSession] in which the session takes place.
   * @param session the [WebSocketSession] used for communication.
   * @param block the [GroupScope] in which the participant may forward some messages.
   */
  suspend fun session(session: WebSocketSession, block: suspend GroupScope.() -> Unit) {
    val peer = join(session)
    try {
      block()
    } finally {
      withContext(NonCancellable) { leave(peer) }
    }
  }
}

/** An interface representing the operations which are available when in a session in a [Group]. */
interface GroupScope {

  /**
   * Forwards a [message] to the given [peer].
   *
   * @param peer the [PeerIdentifier] to which the message should be forwarded.
   * @param message the [ServerToClient] message to forward.
   */
  suspend fun forward(peer: PeerIdentifier, message: ServerToClient)
}
