package io.github.alexandrepiveteau.echo.webrtc.signaling

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * A value class representing the unique identifier for a peer in a group. Peer identifiers are
 * assigned by the signalling server.
 *
 * Within each group, each peer has a unique id.
 *
 * @property id the unique identifier for this peer, within a given group.
 */
@Serializable
@JvmInline
value class PeerIdentifier(val id: Int) {
  override fun toString() = id.toString()
}
