package party.markdown.signaling

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable @JvmInline value class SessionDescription(val json: String)

@Serializable @JvmInline value class IceCandidate(val json: String)

@Serializable @JvmInline value class ChannelId(val id: Int)

@Serializable
sealed class ClientToClientMessage {

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
