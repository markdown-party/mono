package party.markdown.ui.navigator

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import kotlinx.html.classes
import party.markdown.tree.TreeNode
import react.*
import react.dom.div

/**
 * Creates a new [ReactElement] that displays the file navigator, and lets the user perform some
 * manipulations on the different files and folders.
 *
 * @param block the configuration properties for the navigator.
 */
fun RBuilder.navigator(
    block: NavigatorProps.() -> Unit,
): ReactElement = child(navigator) { attrs(block) }

/**
 * An interface defining the properties which can be set to the navigator. This includes the
 * available files, folders, as well as the callbacks which should be called when certain actions
 * are undertaken on the documents.
 */
external interface NavigatorProps : RProps {

  /** The root node that should be displayed in the navigator. */
  var tree: TreeNode
}

private data class Node(
    val indent: Int,
    val key: EventIdentifier,
    val name: String?,
    val folder: Boolean,
)

private fun TreeNode.flatten(): List<Node> {
  // TODO : Iterative traversal.
  fun traverse(node: TreeNode, level: Int, list: MutableList<Node>) {
    when (node) {
      is TreeNode.MarkdownFile -> list.add(Node(level, node.id, node.name, false))
      is TreeNode.Folder -> {
        list.add(Node(level, node.id, node.name, true))
        node.children.sortedBy { it.name }.forEach { traverse(it, level + 1, list) }
      }
    }
  }
  val results = mutableListOf<Node>()
  traverse(this, level = 0, results)
  return results
}

private val navigator =
    functionalComponent<NavigatorProps> { props ->
      val nodes = props.tree.flatten()
      div {
        attrs {
          classes = classes + setOf("flex", "flex-col", "items-start")
          classes = classes + setOf("bg-gray-700", "text-white")
          classes = classes + setOf("p-2")
        }
        for (node in nodes) {
          file {
            key = node.key.toString()
            name = node.name ?: "Unknown"
            isFolder = node.folder
            indentLevel = node.indent
            selected = node.indent == 0
          }
        }
      }
    }
