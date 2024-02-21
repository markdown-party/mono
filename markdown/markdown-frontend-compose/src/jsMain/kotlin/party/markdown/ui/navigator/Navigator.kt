package party.markdown.ui.navigator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toULong
import org.jetbrains.compose.web.dom.Div
import party.markdown.tree.TreeNode
import party.markdown.tree.TreeNodeIdentifier

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
                similar =
                    node.children
                        .asSequence()
                        .minus(node)
                        .filter { n -> n::class == it::class }
                        .filter { n -> n.id < it.id }
                        .filter { n -> n.name == it.name }
                        .count())
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
              similar =
                  root.children
                      .asSequence()
                      .minus(it)
                      .filter { n -> n::class == it::class }
                      .filter { n -> n.id < it.id }
                      .filter { n -> n.name == it.name }
                      .count())
        }
      }
      is TreeNode.MarkdownFile -> Unit // should not happen.
    }
    return results
  }

  return traverseRoot(this)
}

@Composable
fun Navigator(
    tree: TreeNode,
    selected: TreeNode?,
    onTreeNodeSelected: (TreeNode) -> Unit,
    onCreateFile: (parent: TreeNode?) -> Unit,
    onCreateFolder: (parent: TreeNode?) -> Unit,
    onNodeDelete: (TreeNode) -> Unit,
    onNodeRename: (TreeNode, String) -> Unit,
    onNodeMove: (Long, TreeNode) -> Unit,
) {
  val (open, setOpen) = remember { mutableStateOf(emptySet<TreeNodeIdentifier>()) }
  val nodes = tree.flatten(open)
  val (dropdownOpen, setDropdownOpen) = remember { mutableStateOf<TreeNodeIdentifier?>(null) }

  Div(
      attrs = {
        classes("flex", "flex-col", "items-stretch")
        classes("bg-gray-200")
        classes("w-96")
      },
  ) {
    NavigatorActions(
        onClickNewMarkdownFile = { onCreateFile(null) },
        onClickNewFolder = { onCreateFolder(null) },
    )
    for (node in nodes) {
      key(node.key) {
        File(
            displayId = node.key.toULong().toLong(),
            name = node.name ?: "(unnamed)",
            displayName = node.displayName ?: "(unnamed)",
            displayFileType = node.fileType,
            displayIndentLevel = node.indent,
            displaySelected = node.key == selected?.id,
            menuOpen = node.key == dropdownOpen,
            onDropFile = { id -> onNodeMove(id, node.node) },
            onFileClick = {
              when (node.node) {
                is TreeNode.Folder -> {
                  if (node.key in open) {
                    setOpen(open - node.key)
                  } else {
                    setOpen(open + node.key)
                  }
                }
                is TreeNode.MarkdownFile -> {
                  onTreeNodeSelected(node.node)
                }
              }
            },
            onMenuClick = {
              if (dropdownOpen == node.key) setDropdownOpen(null) else setDropdownOpen(node.key)
            },
            onMenuDeleteClick = {
              setDropdownOpen(null)
              onNodeDelete(node.node)
            },
            onRenamed = { onNodeRename(node.node, it) },
            onMenuMoveToParent = {
              setDropdownOpen(null)
              val parent = node.parent ?: tree
              onNodeMove(node.key.toULong().toLong(), parent)
            },
            onMenuCreateFolderClick = {
              setDropdownOpen(null)
              onCreateFolder(node.node)
            },
            onMenuCreateMarkdownClick = {
              setDropdownOpen(null)
              onCreateFile(node.node)
            })
      }
    }
  }
}
