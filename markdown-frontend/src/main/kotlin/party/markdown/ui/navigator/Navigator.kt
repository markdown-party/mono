package party.markdown.ui.navigator

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toULong
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

  var selected: TreeNode?
  var onTreeNodeSelected: (TreeNode) -> Unit

  var onCreateFile: (parent: TreeNode?) -> Unit
  var onCreateFolder: (parent: TreeNode?) -> Unit
  var onNodeDelete: (TreeNode) -> Unit
  var onNodeRename: (TreeNode, String) -> Unit
  var onNodeMove: (Long, TreeNode) -> Unit
}

private data class Node(
    val parent: TreeNode?,
    val node: TreeNode,
    val indent: Int,
    val key: EventIdentifier,
    val name: String?,
    val displayName: String?,
    val fileType: FileType,
)

private fun Set<TreeNode>.sortedByType(): List<TreeNode> {
  val type =
      Comparator<TreeNode> { a, b ->
        when {
          a is TreeNode.Folder && b is TreeNode.Folder -> 0
          a is TreeNode.MarkdownFile && b is TreeNode.MarkdownFile -> 0
          a is TreeNode.Folder -> 1
          else -> -1
        }
      }
  val name =
      Comparator<TreeNode> { a, b ->
        val x = a.name ?: ""
        val y = b.name ?: ""
        x.compareTo(y)
      }
  return sortedWith(type.then(name))
}

private fun TreeNode.flatten(
    open: Set<EventIdentifier>,
): List<Node> {

  // TODO : Iterative traversal.
  fun traverse(
      grandparent: TreeNode?,
      parent: TreeNode?,
      node: TreeNode,
      level: Int,
      list: MutableList<Node>,
      similar: Int,
  ) {
    when (node) {
      is TreeNode.MarkdownFile -> {
        list.add(
            Node(
                parent = grandparent,
                node = node,
                indent = level,
                key = node.id,
                name = node.name,
                displayName = if (similar == 0) "${node.name}.md" else "${node.name} ($similar).md",
                fileType = FileType.Markdown,
            ))
      }
      is TreeNode.Folder -> {
        val type = if (node.id in open) FileType.FolderOpen else FileType.FolderClosed
        list.add(
            Node(
                parent = grandparent,
                node = node,
                indent = level,
                key = node.id,
                name = node.name,
                displayName = if (similar == 0) node.name else "${node.name} ($similar)",
                fileType = type,
            ))
        if (type == FileType.FolderOpen) {
          node.children.sortedByType().forEach {
            traverse(
                grandparent = parent,
                parent = node,
                node = it,
                level = level + 1,
                list = list,
                similar = node.children.asSequence()
                    .minus(node)
                    .filter { n -> n::class == it::class }
                    .filter { n -> n.id < it.id }
                    .filter { n -> n.name == it.name }
                    .count()
            )
          }
        }
      }
    }
  }

  fun traverseRoot(root: TreeNode): List<Node> {
    val results = mutableListOf<Node>()
    when (root) {
      is TreeNode.Folder -> {
        root.children.sortedByType().forEach {
          traverse(
              grandparent = null,
              parent = root,
              node = it,
              level = 0,
              list = results,
              similar = root.children.asSequence()
                  .minus(it)
                  .filter { n -> n::class == it::class }
                  .filter { n -> n.id < it.id }
                  .filter { n -> n.name == it.name }
                  .count()
          )
        }
      }
      is TreeNode.MarkdownFile -> Unit // should not happen.
    }
    return results
  }

  return traverseRoot(this)
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
          w-96
          """,
      ) {
        navigatorActions {
          onClickNewMarkdownFile = { props.onCreateFile(null) }
          onClickNewFolder = { props.onCreateFolder(null) }
        }
        for (node in nodes) {
          file {
            key = node.key.toString()
            displayId = node.key.toULong().toLong()
            name = node.name ?: "(unnamed)"
            displayName = node.displayName ?: "(unnamed)"
            displayFileType = node.fileType
            displayIndentLevel = node.indent
            displaySelected = node.key == props.selected?.id
            menuOpen = node.key == dropdownOpen
            onDropFile = { id -> props.onNodeMove(id, node.node) }
            onFileClick =
                {
                  when (node.node) {
                    is TreeNode.Folder -> {
                      if (node.key in open) {
                        setOpen(open - node.key)
                      } else {
                        setOpen(open + node.key)
                      }
                    }
                    is TreeNode.MarkdownFile -> {
                      props.onTreeNodeSelected(node.node)
                    }
                  }
                }
            onMenuClick =
                {
                  if (dropdownOpen == node.key) setDropdownOpen(null) else setDropdownOpen(node.key)
                }
            onMenuDeleteClick =
                {
                  setDropdownOpen(null)
                  props.onNodeDelete(node.node)
                }
            onRenamed = { props.onNodeRename(node.node, it) }
            onMenuMoveToParent =
                {
                  setDropdownOpen(null)
                  val parent = node.parent ?: props.tree
                  props.onNodeMove(node.key.toULong().toLong(), parent)
                }
            onMenuCreateFolderClick =
                {
                  setDropdownOpen(null)
                  props.onCreateFolder(node.node)
                }
            onMenuCreateMarkdownClick =
                {
                  setDropdownOpen(null)
                  props.onCreateFile(node.node)
                }
          }
        }
      }
    }
