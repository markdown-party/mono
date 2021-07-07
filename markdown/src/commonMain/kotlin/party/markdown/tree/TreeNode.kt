package party.markdown.tree

/**
 * A sealed class representing an immutable view of the tree structure. Nodes can be of two kinds :
 * [MarkdownFile] and [Folder].
 *
 * A [TreeNode] is guaranteed to be in a consistent state, and respect tree invariants.
 */
sealed interface TreeNode {

  /** The unique identifier for this [TreeNode]. */
  val id: TreeNodeIdentifier

  /** The display name for this [TreeNode]. */
  val name: String?

  /**
   * A folder that contains a [List] of [children].
   *
   * @param id the identifier for this folder.
   * @param children the [List] of [TreeNode] that act as the children of this [TreeNode].
   * @param name the display name for this folder.
   */
  data class Folder(
      override val id: TreeNodeIdentifier,
      val children: Set<TreeNode>,
      override val name: String?,
  ) : TreeNode

  /**
   * A document that contains some text, in Markdown.
   *
   * @param id the identifier for this file.
   * @param name the display name for this file.
   */
  data class MarkdownFile(
      override val id: TreeNodeIdentifier,
      override val name: String?,
  ) : TreeNode
}
