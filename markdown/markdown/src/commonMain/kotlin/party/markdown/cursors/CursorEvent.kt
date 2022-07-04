package party.markdown.cursors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import party.markdown.rga.RGANodeIdentifier
import party.markdown.tree.TreeNodeIdentifier

/**
 * A [CursorAnchorIdentifier] identifies the anchor after which the cursor gets positioned. When
 * moved, the cursor will be placed directly after an existing [RGANodeIdentifier].
 */
typealias CursorAnchorIdentifier = RGANodeIdentifier

/** The initial anchor for the cursor, when no events have been issued yet. */
val CursorRoot = RGANodeIdentifier.Unspecified

/**
 * Events that the user may perform in order to move the cursor. The user may only have a single
 * cursor across all the documents.
 */
@Serializable
@SerialName("c")
sealed class CursorEvent {

  /**
   * Moves the cursor after the given [anchor] in the given [node]. The cursor may only be present
   * in a single [node] (or file) at a time, and will therefore be removed from any other node
   * whenever it is moved.
   *
   * @param node the file in which the cursor is present.
   * @param anchor the character after which the cursor is moved.
   */
  @Serializable
  @SerialName("c:ma")
  data class MoveAfter(
      val node: TreeNodeIdentifier,
      val anchor: CursorAnchorIdentifier,
  ) : CursorEvent()
}
