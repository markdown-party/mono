package io.github.alexandrepiveteau.echo.webrtc.signaling

import kotlinx.serialization.Serializable

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
     * @property message the message to be transmitted.
     */
    @Serializable
    data class Forward(
        override val to: PeerIdentifier,
        val message: ClientToClientMessage,
    ) : ClientToServer() {

      override fun toServerToClient(
          from: PeerIdentifier,
      ) = ServerToClient.GotMessage(from = from, message = message)
    }
  }

  /** A marker interface for messages sent from the server to the browser. */
  @Serializable
  sealed class ServerToClient : SignalingMessage() {

    /**
     * Indicates an ICE candidate to use when communicating with a given client.
     *
     * @property from the identifier of the peer who sent the message.
     * @property message the message received from the peer.
     */
    @Serializable
    data class GotMessage(
        val from: PeerIdentifier,
        val message: ClientToClientMessage,
    ) : ServerToClient()

    /**
     * Indicates that a peer has joined the collaboration session. The client should attempt to
     * connect to this new peer.
     *
     * @property peer the unique identifier of the peer.
     */
    @Serializable data class PeerJoined(val peer: PeerIdentifier) : ServerToClient()

    /**
     * Indicates that a peer has left the collaboration session. The client should stop
     * collaboration with the provided peer.
     *
     * @property peer the unique identifier of the peer.
     */
    @Serializable data class PeerLeft(val peer: PeerIdentifier) : ServerToClient()
  }
}