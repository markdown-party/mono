package party.markdown.ui.navigator

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import party.markdown.tree.TreeNode
import party.markdown.tree.TreeNodeIdentifier
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

  var onCreateFile: () -> Unit
  var onCreateFolder: () -> Unit
  var onNodeDelete: (TreeNode) -> Unit
  var onNodeRename: (TreeNode) -> Unit
}

private data class Node(
    val node: TreeNode,
    val indent: Int,
    val key: EventIdentifier,
    val name: String?,
    val folder: Boolean,
)

private fun TreeNode.flatten(
    open: Set<EventIdentifier>,
): List<Node> {
  // TODO : Iterative traversal.
  fun traverse(node: TreeNode, level: Int, list: MutableList<Node>) {
    when (node) {
      is TreeNode.MarkdownFile -> list.add(Node(node, level, node.id, node.name, false))
      is TreeNode.Folder -> {
        list.add(Node(node, level, node.id, node.name, true))

        // Add the children, but only if this folder is open.
        if (node.id in open) {
          node.children.sortedBy { it.name }.forEach { traverse(it, level + 1, list) }
        }
      }
    }
  }
  val results = mutableListOf<Node>()
  traverse(this, level = 0, results)
  return results
}

private val navigator =
    functionalComponent<NavigatorProps> { props ->
      val (open, setOpen) = useState(emptySet<TreeNodeIdentifier>())
      val nodes = props.tree.flatten(open)
      val (dropdownOpen, setDropdownOpen) = useState<TreeNodeIdentifier?>(null)

      div(
          """
          flex flex-col items-stretch
          bg-gray-700 text-white
          w-1/6
          """,
      ) {
        navigatorActions {
          onClickNewMarkdownFile = props.onCreateFile
          onClickNewFolder = props.onCreateFolder
        }
        for (node in nodes) {
          file {
            key = node.key.toString()
            displayName = node.name ?: "(unnamed)"
            displayFileType =
                when {
                  node.folder && node.key in open -> FileType.FolderOpen
                  node.folder -> FileType.FolderClosed
                  else -> FileType.Markdown
                }
            displayIndentLevel = node.indent
            displaySelected = node.indent == 0
            menuOpen = node.key == dropdownOpen
            onFileClick =
                {
                  if (node.node is TreeNode.Folder) {
                    if (node.key in open) {
                      setOpen(open - node.key)
                    } else {
                      setOpen(open + node.key)
                    }
                  }
                }
            onMenuClick =
                {
                  if (dropdownOpen == node.key) setDropdownOpen(null) else setDropdownOpen(node.key)
                }
            onMenuDeleteClick =
                {
                  if (node.key == dropdownOpen) setDropdownOpen(null)
                  props.onNodeDelete(node.node)
                }
            onMenuRenameClick =
                {
                  if (node.key == dropdownOpen) setDropdownOpen(null)
                  props.onNodeRename(node.node)
                }
          }
        }
      }
    }
