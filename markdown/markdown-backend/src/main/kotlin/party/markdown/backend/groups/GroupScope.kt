package party.markdown.backend.groups

import io.github.alexandrepiveteau.echo.webrtc.signaling.PeerIdentifier
import io.github.alexandrepiveteau.echo.webrtc.signaling.SignalingMessage.ServerToClient

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
