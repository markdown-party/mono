package io.github.alexandrepiveteau.echo.webrtc.client

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.ReceiveExchange
import io.github.alexandrepiveteau.echo.SendExchange
import io.github.alexandrepiveteau.echo.asReceiveExchange
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.github.alexandrepiveteau.echo.webrtc.client.internal.encode
import io.github.alexandrepiveteau.echo.webrtc.signaling.PeerIdentifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * An interface representing a signaling server, which provides information about the currently
 * connected peers, as well as ways to create a [ReceiveExchange] with any of the remote peers from
 * the signaling server.
 */
interface SignalingServer {

  /** A [Flow] which indicates currently available [Peer]s. */
  val peers: SharedFlow<Set<Peer>>
}

/**
 * Syncs the [SendExchange] to the peers available in the [SignalingServer]. The synchronisation
 * process won't terminate, but may be cancelled or throw an exception if the communication channel
 * gets closed.
 *
 * @receiver the [SignalingServer] which provides information about the sites.
 * @param exchange the [ReceiveExchange] to which we're interested in syncing.
 * @param onParticipantsChanged a callback which will be called when the participant count changes.
 */
suspend fun SignalingServer.sync(
    exchange: Exchange<Incoming, Outgoing>,
    onParticipantsChanged: (Set<PeerIdentifier>) -> Unit = {},
): Nothing = coroutineScope {
  val connected = mutableMapOf<Peer, Job>()

  // Preserve state for remaining sites across collect invocations, such that existing jobs are
  // preserved and outdated jobs are cancelled appropriately. Existing jobs are preserved, so
  // the ongoing connection remains active.
  peers.collect { peers ->
    val added = peers - connected.keys
    val removed = connected.keys - peers

    // Remove all the outdated peers.
    for (peer in removed) {
      connected -= peer
      peer.cancel()
    }

    // Add all the new peers.
    for (peer in added) {
      connected[peer] = launch { exchange.encode().asReceiveExchange().sync(peer) }
    }

    // Notify the callback.
    onParticipantsChanged(peers.mapTo(mutableSetOf()) { it.identifier })
  }
}
