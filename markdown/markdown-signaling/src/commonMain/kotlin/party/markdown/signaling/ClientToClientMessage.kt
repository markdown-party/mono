package party.markdown.signaling

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/** A session description, encoded in [json]. */
@Serializable @JvmInline value class SessionDescription(val json: String)

/** An ICE candidate, encoded in [json]. */
@Serializable @JvmInline value class IceCandidate(val json: String)

/**
 * A channel identifier, which lets clients multiplex over a single connection and which has a
 * unique [id].
 */
@Serializable @JvmInline value class ChannelId(val id: Int)

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
