package party.markdown.p2p

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

/**
 * An interface representing a connection with a remote peer. A [PeerToPeerConnection] consists in
 * two channels, which buffer messages to be sent and received from the other destination.
 */
interface PeerToPeerConnection {

  /** The [ReceiveChannel] representing the messages coming from the other peer. */
  val incoming: ReceiveChannel<String>

  /** The [SendChannel] representing the messages going to the other peer. */
  val outgoing: SendChannel<String>
}
