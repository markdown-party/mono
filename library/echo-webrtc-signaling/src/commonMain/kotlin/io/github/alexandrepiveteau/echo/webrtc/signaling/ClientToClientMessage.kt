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

  /**
   * A message which indicates an offer from one client to another.
   *
   * @param channel the [ChannelId] for this offer.
   * @param offer the [SessionDescription] of the offer.
   */
  @Serializable
  data class Offer(
      override val channel: ChannelId,
      val offer: SessionDescription,
  ) : ClientToClientMessage()

  /**
   * A message which indicates an answer from one client to an offer.
   *
   * @param channel the [ChannelId] of the offer to which we're responding.
   * @param answer the [SessionDescription] of the answer.
   */
  @Serializable
  data class Answer(
      override val channel: ChannelId,
      val answer: SessionDescription,
  ) : ClientToClientMessage()

  /**
   * A message which wraps an [IceCandidate] from the side which issued an [Offer].
   *
   * @param channel the [ChannelId] of the original offer.
   * @param ice the [IceCandidate] which is sent.
   */
  @Serializable
  data class IceCaller(
      override val channel: ChannelId,
      val ice: IceCandidate,
  ) : ClientToClientMessage()

  /**
   * A message which wraps an [IceCandidate] from the side which issued an [Answer].
   *
   * @param channel the [ChannelId] of the original offer and answer.
   * @param ice the [IceCandidate] which is sent.
   */
  @Serializable
  data class IceCallee(
      override val channel: ChannelId,
      val ice: IceCandidate,
  ) : ClientToClientMessage()
}
