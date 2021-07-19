package party.markdown

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import party.markdown.cursors.CursorEvent
import party.markdown.rga.RGAEvent
import party.markdown.tree.TreeEvent
import party.markdown.tree.TreeNodeIdentifier

@SerialName("e")
@Serializable
sealed class MarkdownPartyEvent {

  @SerialName("u")
  @Serializable
  data class Cursor(
      val event: CursorEvent,
  ) : MarkdownPartyEvent()

  @SerialName("t")
  @Serializable
  data class Tree(
      val event: TreeEvent,
  ) : MarkdownPartyEvent()

  @SerialName("c")
  @Serializable
  data class RGA(
      val document: TreeNodeIdentifier,
      val event: RGAEvent,
  ) : MarkdownPartyEvent()
}
