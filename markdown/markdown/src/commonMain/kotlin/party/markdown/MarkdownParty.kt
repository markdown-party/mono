package party.markdown

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import party.markdown.cursors.Cursors
import party.markdown.tree.TreeNode
import party.markdown.tree.TreeNodeIdentifier

data class MarkdownParty(
    val cursors: Cursors,
    val tree: TreeNode,
    val documents: Map<TreeNodeIdentifier, Pair<CharArray, EventIdentifierArray>>,
)
