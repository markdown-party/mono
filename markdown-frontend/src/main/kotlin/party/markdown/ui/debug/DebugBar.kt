package party.markdown.ui.debug

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.protocol.Message
import kotlinx.html.js.onClickFunction
import party.markdown.react.useSync
import party.markdown.tree.TreeEvent
import party.markdown.tree.TreeNode
import react.*
import react.dom.button
import react.dom.div
import react.dom.p
import react.dom.span

fun RBuilder.debugBar(
    block: DebugBarProps.() -> Unit,
): ReactElement = child(bar) { attrs(block) }

external interface DebugBarProps : RProps {
  var local: MutableSite<TreeEvent, TreeNode>
  var remote: Exchange<Message.Incoming, Message.Outgoing>
}

private fun RBuilder.button(
    text: String,
    action: () -> Unit,
): ReactElement {
  return button(
      classes =
          "transition-all text-white px-4 py-2 bg-blue-500 hover:bg-blue-600 rounded shadow hover:shadow-lg") {
    attrs { onClickFunction = { action() } }
    +text
  }
}

private val bar =
    functionalComponent<DebugBarProps> { props ->
      val (syncing, requestSync, stopSync) = useSync(props.local, props.remote)
      div(classes = "flex flex-row items-center h-24 space-x-8") {
        p(classes = "px-4 py-2 font-medium space-x-2") {
          span { +"Syncing :" }
          span(
              classes =
                  if (syncing) "font-black text-green-500"
                  else "font-black text-bold text-red-500") { +"$syncing" }
        }
        button("Sync now", requestSync)
        button("Stop sync", stopSync)
      }
    }
