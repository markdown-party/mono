package party.markdown.ui

import kotlin.random.Random
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

private val panes =
    functionalComponent<PanesProps> { props ->
      val tree = useFlow(props.treeApi.current)
      val scope = useCoroutineScope()

      div(classes = "h-full flex flex-row") {
        navigator {
          this.tree = tree
          this.onCreateFile =
              {
                scope.launch { props.treeApi.createFile("File ${Random.nextInt(10)}", it ?: tree) }
              }
          this.onCreateFolder =
              {
                scope.launch {
                  props.treeApi.createFolder("Folder ${Random.nextInt(10)}", it ?: tree)
                }
              }
          this.onNodeRename =
              { node ->
                scope.launch { props.treeApi.name("Named ${Random.nextInt(10)}", node) }
              }
          this.onNodeDelete = { node -> scope.launch { props.treeApi.remove(node) } }
        }
        editor {}
      }
    }
