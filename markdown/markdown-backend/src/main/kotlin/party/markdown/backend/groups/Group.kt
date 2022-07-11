package party.markdown.backend.groups

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import party.markdown.coroutines.inOrder
import party.markdown.signaling.PeerIdentifier
import party.markdown.signaling.SignalingMessage.ServerToClient
import party.markdown.signaling.SignalingMessage.ServerToClient.PeerJoined

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
          site.sendCatching(PeerJoined(peer)) // Tell other sites we've joined.
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
      val outbox = peers[peer]
      withNoResult { outbox?.sendCatching(message) }
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
