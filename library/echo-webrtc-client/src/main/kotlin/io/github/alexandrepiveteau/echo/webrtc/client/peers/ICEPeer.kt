package io.github.alexandrepiveteau.echo.webrtc.client.peers

import io.github.alexandrepiveteau.echo.webrtc.client.Peer
import io.github.alexandrepiveteau.echo.webrtc.signaling.IceCandidate

/** A specialized [Peer] which may accept some [IceCandidate] to establish a connection. */
internal interface ICEPeer : Peer {

  /**
   * Adds an [IceCandidate] to this peer
   *
   * @param candidate the new candidate.
   */
  suspend fun ice(candidate: IceCandidate)
}
