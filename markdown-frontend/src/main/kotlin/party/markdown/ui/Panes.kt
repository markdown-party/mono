package party.markdown.ui

import io.github.alexandrepiveteau.echo.core.causality.toEventIdentifier
import kotlinx.coroutines.launch
import party.markdown.data.tree.TreeApi
import party.markdown.react.useCoroutineScope
import party.markdown.react.useFlow
import party.markdown.ui.editor.editor
import party.markdown.ui.navigator.navigator
import react.*
import react.dom.div

fun RBuilder.panes(
    block: PanesProps.() -> Unit,
): ReactElement = child(panes) { attrs(block) }

external interface PanesProps : RProps {
  var treeApi: TreeApi
}

private fun nextName(): String = buildString { repeat(3) { append(('a'..'z').random()) } }

private val panes =
    functionalComponent<PanesProps> { props ->
      val tree = useFlow(props.treeApi.current)
      val scope = useCoroutineScope()

      div(classes = "h-full flex flex-row") {
        navigator {
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
              { node ->
                scope.launch {
                  val newName =
                      if (node.name?.endsWith(".md") == true) {
                        "${nextName()}.md"
                      } else {
                        nextName()
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
        editor {}
      }
    }
