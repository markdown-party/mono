package party.markdown.p2p

import io.github.alexandrepiveteau.echo.ReceiveExchange
import kotlinx.coroutines.flow.*
import party.markdown.signaling.PeerIdentifier

/**
 * An interface representing a signaling server, which provides information about the currently
 * connected peers, as well as ways to create a [ReceiveExchange] with any of the remote peers from
 * the signaling server.
 */
interface SignalingServer {

  /** A [Flow] which indicates currently available peers. */
  val peers: SharedFlow<Set<PeerIdentifier>>

  /**
   * Connects to a remote peer with the given identifier, and returns the associated
   * [PeerToPeerConnection].
   *
   * @param peer the [PeerIdentifier] that we are interested in.
   * @return the resulting [PeerToPeerConnection].
   */
  suspend fun connect(peer: PeerIdentifier): PeerToPeerConnection
}
