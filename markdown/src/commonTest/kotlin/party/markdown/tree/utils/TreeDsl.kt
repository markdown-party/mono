package party.markdown.tree.utils

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import party.markdown.tree.TreeNode
import party.markdown.tree.TreeNodeRoot

@DslMarker annotation class TreeDsl

@TreeDsl
interface TreeScope {
  fun file(id: EventIdentifier, name: String? = null)
  fun folder(
      id: EventIdentifier,
      name: String? = null,
      children: TreeScope.() -> Unit,
  )
}

/**
 * Builds a new [TreeNode] with the DSL. This is just a set of convenience methods to make building
 * complex trees easier in tests.
 */
fun tree(
    name: String? = null,
    children: TreeScope.() -> Unit,
): TreeNode {
  return TreeNode.Folder(
      id = TreeNodeRoot,
      children = buildRecursively(children),
      name = name,
  )
}

private fun buildRecursively(
    children: TreeScope.() -> Unit,
): Set<TreeNode> {
  val result = mutableSetOf<TreeNode>()
  val scope =
      object : TreeScope {
        override fun file(
            id: EventIdentifier,
            name: String?,
        ) {
          result.add(TreeNode.MarkdownFile(id, name))
        }
        override fun folder(
            id: EventIdentifier,
            name: String?,
            children: TreeScope.() -> Unit,
        ) {
          result.add(TreeNode.Folder(id, buildRecursively(children), name))
        }
      }
  children(scope)
  return result
}
