package party.markdown

import party.markdown.tree.TreeNode
import party.markdown.tree.TreeNodeIdentifier

data class MarkdownParty(
    val tree: TreeNode,
    val documents: Map<TreeNodeIdentifier, String>,
)
