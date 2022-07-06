package party.markdown.signaling

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
@Serializable @JvmInline value class PeerIdentifier(val id: Int)

/**
 * A value class representing the description of a session.
 *
 * @property json the encoded description.
 */
@Serializable @JvmInline value class SessionDescription(val json: String)

/**
 * A value class representing the description of a candidate.
 *
 * @property json the encoded description.
 */
@Serializable @JvmInline value class IceCandidate(val json: String)

/**
 * Represents the different messages which are supported by the signalling server.
 *
 * The signalling server keeps track of the different clients which are connected for each session,
 * and helps them exchange ICE candidates until a peer-to-peer connection is established. It also
 * communicates new connections, and clients which dropped their connection.
 */
@Serializable
sealed class SignalingMessage {

  /** A marker interface for messages sent from the browser to the server. */
  @Serializable
  sealed class ClientToServer : SignalingMessage() {

    /** The [PeerIdentifier] of the peer to which the message should be forwarded. */
    abstract val to: PeerIdentifier

    /**
     * Transforms this [ClientToServer] message to a [ServerToClient] message, using the provided
     * [PeerIdentifier].
     *
     * @param from the originator [PeerIdentifier].
     * @return the resulting [ServerToClient].
     */
    abstract fun toServerToClient(from: PeerIdentifier): ServerToClient

    /**
     * Indicates that the server should relay this ICE candidate to the given peer.
     *
     * @property to the identifier of the peer who should receive the message.
     * @property iceCandidate the encoded ICE candidate information.
     */
    @Serializable
    data class ForwardIceCandidate(
        override val to: PeerIdentifier,
        val iceCandidate: IceCandidate,
    ) : ClientToServer() {

      override fun toServerToClient(
          from: PeerIdentifier,
      ) = ServerToClient.GotIceCandidate(from, iceCandidate)
    }

    /**
     * Indicates that the server should relay the session description to the given peer.
     *
     * @property to the identifier of the peer who should receive the message.
     * @property description the encoded session description.
     */
    @Serializable
    data class ForwardSessionDescription(
        override val to: PeerIdentifier,
        val description: SessionDescription,
    ) : ClientToServer() {

      override fun toServerToClient(
          from: PeerIdentifier,
      ) = ServerToClient.GotSessionDescription(from, description)
    }
  }

  /** A marker interface for messages sent from the server to the browser. */
  @Serializable
  sealed class ServerToClient : SignalingMessage() {

    /**
     * Indicates a session description to use when communicating with a given client.
     *
     * @property from the identifier o the peer who sent the message.
     * @property description the encoded session description.
     */
    @Serializable
    data class GotSessionDescription(
        val from: PeerIdentifier,
        val description: SessionDescription,
    ) : ServerToClient()

    /**
     * Indicates an ICE candidate to use when communicating with a given client.
     *
     * @property from the identifier of the peer who sent the message.
     * @property iceCandidate the encoded ICE candidate information.
     */
    @Serializable
    data class GotIceCandidate(
        val from: PeerIdentifier,
        val iceCandidate: IceCandidate,
    ) : ServerToClient()

    /**
     * Indicates that a peer has joined the collaboration session. The client should attempt to
     * connect to this new peer.
     *
     * @property peer the unique identifier of the peer.
     */
    @Serializable
    data class PeerJoined(
        val peer: PeerIdentifier,
    ) : ServerToClient()

    /**
     * Indicates that a peer has left the collaboration session. The client should stop
     * collaboration with the provided peer.
     *
     * @property peer the unique identifier of the peer.
     */
    @Serializable data class PeerLeft(val peer: PeerIdentifier) : ServerToClient()
  }
}
