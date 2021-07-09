package party.markdown

import kotlinx.serialization.Serializable
import party.markdown.rga.RGAEvent
import party.markdown.tree.TreeEvent
import party.markdown.tree.TreeNodeIdentifier

@Serializable
sealed class MarkdownPartyEvent {

  @Serializable
  data class Tree(
      val event: TreeEvent,
  ) : MarkdownPartyEvent()

  @Serializable
  data class RGA(
      val document: TreeNodeIdentifier,
      val event: RGAEvent,
  ) : MarkdownPartyEvent()
}
