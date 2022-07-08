package party.markdown.p2p

import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json

val DefaultStringFormat: StringFormat = Json

@Serializable value class SessionDescription(val json: String)

@Serializable value class IceCandidate(val json: String)

@Serializable value class ChannelId(val id: Int)

@Serializable
sealed class Message {

  abstract val channel: ChannelId

  @Serializable
  data class Offer(
      override val channel: ChannelId,
      val offer: SessionDescription,
  ) : Message()

  @Serializable
  data class Answer(
      override val channel: ChannelId,
      val answer: SessionDescription,
  ) : Message()

  @Serializable
  data class IceCaller(
      override val channel: ChannelId,
      val ice: IceCandidate,
  ) : Message()

  @Serializable
  data class IceCallee(
      override val channel: ChannelId,
      val ice: IceCandidate,
  ) : Message()
}
