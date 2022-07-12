package io.github.alexandrepiveteau.echo.webrtc.server.groups

import io.github.alexandrepiveteau.echo.webrtc.server.SessionIdentifier
import io.github.alexandrepiveteau.echo.webrtc.server.coroutines.actor
import io.github.alexandrepiveteau.echo.webrtc.signaling.PeerIdentifier
import io.github.alexandrepiveteau.echo.webrtc.signaling.SignalingMessage.ServerToClient
import io.github.alexandrepiveteau.echo.webrtc.signaling.SignalingMessage.ServerToClient.PeerJoined
import io.ktor.util.logging.*
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * A group represents a session of collaboration. Each participant will be granted a unique
 * identifier, and will be notified of new members.
 *
 * @param scope the [CoroutineScope] in which the [Group] is running.
 * @param session the [SessionIdentifier] in which the [Group] is running.
 * @param logger the [Logger] which is used to monitor the [Group].
 */
internal class Group(
    private val scope: CoroutineScope,
    private val session: SessionIdentifier,
    private val logger: Logger,
) {

  /** Logs the given [message], prefixing it with the [session] identifier. */
  private fun log(message: String) {
    logger.debug("($session) $message")
  }

  /** The `Actor` used to schedule all the computations. */
  private val actor = scope.actor()

  /** The [MutableMap] of all the peer identifiers, and the [Outbox] for their messages. */
  private var peers = persistentMapOf<PeerIdentifier, Outbox<ServerToClient>>()

  /** The next [PeerIdentifier] that should be given. */
  private var _nextId = 0

  /** Returns the next [PeerIdentifier] that should be attributed. */
  private fun nextPeerIdentifier() = PeerIdentifier(_nextId++)

  /**
   * Joins the [Group], and gets a new peer identifier assigned.
   *
   * @param outbox the [Outbox] for this participant.
   * @return the [PeerIdentifier] assigned to this participant.
   */
  private suspend fun join(outbox: Outbox<ServerToClient>): PeerIdentifier {
    return actor.schedule {
      val current = peers
      val peer = nextPeerIdentifier()
      peers = current.put(peer, outbox)
      withResult(peer) {
        for ((id, site) in current) {
          log("[->$id] : $peer joined.")
          site.sendCatching(PeerJoined(peer)) // Tell other sites we've joined.
          log("[->$peer] : $id joined.")
          outbox.sendCatching(PeerJoined(id)) // Learn all the connected sites.
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
    actor.schedule {
      val withoutPeer = peers.remove(peer)
      peers = withoutPeer
      withNoResult {
        for ((to, outbox) in withoutPeer) {
          log("[->$to] : $peer left.")
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
    actor.schedule {
      val outbox = peers[peer]
      withNoResult {
        if (outbox != null) {
          log("[->$peer] : $message")
          outbox.sendCatching(message)
        }
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
