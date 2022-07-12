package io.github.alexandrepiveteau.echo.webrtc.signaling

import kotlinx.serialization.Serializable

/**
 * A sealed class representing the different kinds of messages which may be exchanged between
 * clients. These messages are sent using a [SignalingMessage.ClientToServer.Forward] message.
 */
@Serializable
sealed class ClientToClientMessage {

  /** The identifier for the channel in which this [ClientToClientMessage] is sent. */
  abstract val channel: ChannelId

  @Serializable
  data class Offer(
      override val channel: ChannelId,
      val offer: SessionDescription,
  ) : ClientToClientMessage()

  @Serializable
  data class Answer(
      override val channel: ChannelId,
      val answer: SessionDescription,
  ) : ClientToClientMessage()

  @Serializable
  data class IceCaller(
      override val channel: ChannelId,
      val ice: IceCandidate,
  ) : ClientToClientMessage()

  @Serializable
  data class IceCallee(
      override val channel: ChannelId,
      val ice: IceCandidate,
  ) : ClientToClientMessage()
}
