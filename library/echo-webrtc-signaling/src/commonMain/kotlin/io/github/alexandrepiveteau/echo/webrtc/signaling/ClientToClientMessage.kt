package io.github.alexandrepiveteau.echo.webrtc.signaling

import kotlinx.serialization.Serializable

/**
 * A sealed class representing the different kinds of messages which may be exchanged between
 * clients. These messages are sent using a [SignalingMessage.ClientToServer.Forward] message.
 */
@Serializable
sealed class ClientToClientMessage {

  /**
   * A message which indicates an offer from one client to another.
   *
   * @param offer the [SessionDescription] of the offer.
   */
  @Serializable data class Offer(val offer: SessionDescription) : ClientToClientMessage()

  /**
   * A message which indicates an answer from one client to an offer.
   *
   * @param answer the [SessionDescription] of the answer.
   */
  @Serializable data class Answer(val answer: SessionDescription) : ClientToClientMessage()

  /**
   * A message which wraps an [IceCandidate] from the side which issued an [Offer].
   *
   * @param ice the [IceCandidate] which is sent.
   */
  @Serializable data class IceCaller(val ice: IceCandidate) : ClientToClientMessage()

  /**
   * A message which wraps an [IceCandidate] from the side which issued an [Answer].
   *
   * @param ice the [IceCandidate] which is sent.
   */
  @Serializable data class IceCallee(val ice: IceCandidate) : ClientToClientMessage()
}
