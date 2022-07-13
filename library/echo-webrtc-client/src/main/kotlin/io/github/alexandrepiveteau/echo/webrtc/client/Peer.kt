package io.github.alexandrepiveteau.echo.webrtc.client

import io.github.alexandrepiveteau.echo.webrtc.signaling.PeerIdentifier

/**
 * A [Peer] represents a remote peer, and the associated [PeerToPeerConnection]. Peers have a
 * lifecycle, and may be cancelled when they're not reachable anymore.
 */
interface Peer : PeerToPeerConnection {

  /** The unique [PeerIdentifier] for this [Peer]. */
  val identifier: PeerIdentifier

  /** Cancels the [Peer], and stops all its synchronisation jobs. */
  fun cancel()
}
