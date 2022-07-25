package party.markdown.ui

import androidx.compose.runtime.*
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier.Companion.Unspecified
import io.github.alexandrepiveteau.echo.core.causality.toEventIdentifier
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.Div
import party.markdown.data.text.TextApi
import party.markdown.data.tree.TreeApi
import party.markdown.tree.TreeNode
import party.markdown.tree.TreeNode.Folder
import party.markdown.ui.editor.Editor
import party.markdown.ui.navigator.Navigator

private val Names =
    listOf(
        "Lamport",
        "Liskov",
        "Naur",
        "Pnueli",
        "Kay",
        "Rivest",
        "Tarjan",
        "Turing",
    )

private val Adjectives =
    listOf(
        "Friendly",
        "Happy",
        "Surprised",
        "Wonderful",
        "Amazing",
        "Hungry",
        "Incredible",
        "Famous",
    )

private fun name(): String {
  return "${Adjectives.random()} ${Names.random()}"
}

@Composable
fun Panes(
    treeApi: TreeApi,
    textApi: TextApi,
) {
  val tree by treeApi.current.collectAsState(Folder(Unspecified, emptySet(), null))
  val scope = rememberCoroutineScope()
  val (selected, setSelected) = remember { mutableStateOf<TreeNode?>(null) }

  Div(
      attrs = { classes("h-full", "flex", "flex-row") },
  ) {
    Navigator(
        selected = selected,
        onTreeNodeSelected = setSelected,
        tree = tree,
        onCreateFile = { scope.launch { treeApi.createFile(name(), it ?: tree) } },
        onCreateFolder = { scope.launch { treeApi.createFolder(name(), it ?: tree) } },
        onNodeRename = { node, name -> scope.launch { treeApi.name(name, node) } },
        onNodeDelete = { node -> scope.launch { treeApi.remove(node) } },
        onNodeMove = { id, node ->
          scope.launch { treeApi.move(id.toULong().toEventIdentifier(), node) }
        })
    DividerHorizontal()
    Editor(
        treeNode = selected?.takeIf { it in tree },
        api = textApi,
    )
  }
}
