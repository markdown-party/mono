package party.markdown.ui

import io.github.alexandrepiveteau.echo.core.causality.toEventIdentifier
import kotlinx.coroutines.launch
import party.markdown.data.text.TextApi
import party.markdown.data.tree.TreeApi
import party.markdown.react.useCoroutineScope
import party.markdown.react.useFlow
import party.markdown.tree.TreeNode
import party.markdown.ui.editor.editor
import party.markdown.ui.navigator.navigator
import react.*
import react.dom.div

fun RBuilder.panes(
    block: PanesProps.() -> Unit,
): ReactElement = child(panes) { attrs(block) }

external interface PanesProps : RProps {
  var treeApi: TreeApi
  var textApi: TextApi
}

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

private fun nextName(): String {
  return "${Adjectives.random()} ${Names.random()}"
}

private val panes =
    functionalComponent<PanesProps> { props ->
      val tree = useFlow(props.treeApi.current)
      val scope = useCoroutineScope()
      val (selected, setSelected) = useState<TreeNode?>(null)
      val nodes = useFlow(props.treeApi.current)

      div(classes = "h-full flex flex-row") {
        navigator {
          this.selected = selected
          this.onTreeNodeSelected = setSelected

          this.tree = tree
          this.onCreateFile =
              {
                scope.launch { props.treeApi.createFile("${nextName()}.md", it ?: tree) }
              }
          this.onCreateFolder =
              {
                scope.launch { props.treeApi.createFolder(nextName(), it ?: tree) }
              }
          this.onNodeRename =
              { node, name ->
                scope.launch {
                  val newName =
                      if (name.endsWith(".md") || node is TreeNode.Folder) {
                        name
                      } else {
                        "$name.md"
                      }
                  props.treeApi.name(newName, node)
                }
              }
          this.onNodeDelete = { node -> scope.launch { props.treeApi.remove(node) } }
          this.onNodeMove =
              { id, node ->
                scope.launch {
                  props.treeApi.move(
                      id.toULong().toEventIdentifier(),
                      node,
                  )
                }
              }
        }
        dividerHorizontal()
        editor {
          node = selected?.takeIf { it in nodes }
          api = props.textApi
        }
      }
    }
