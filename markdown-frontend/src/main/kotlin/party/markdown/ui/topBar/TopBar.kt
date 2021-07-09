package party.markdown.ui.topBar

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.protocol.Message
import kotlinx.html.js.onClickFunction
import party.markdown.tree.TreeEvent
import party.markdown.tree.TreeNode
import react.*
import react.dom.button
import react.dom.div
import react.dom.img

fun RBuilder.topBar(
    block: TopBarProps.() -> Unit,
): ReactElement = child(component) { attrs(block) }

external interface TopBarProps : RProps {
  var local: MutableSite<TreeEvent, TreeNode>
  var remote: Exchange<Message.Incoming, Message.Outgoing>
  var onDebugEnabled: (Boolean) -> Unit
  var debugEnabled: Boolean
}

private val component =
    functionalComponent<TopBarProps> { props ->
      div("p-4 bg-gray-800 text-white flex flex-row items-center") {
        img(
            src = "/img/logo.svg",
            classes = "h-12 cursor-pointer hover:bg-gray-600 rounded px-2 py-1") {
          attrs { onClickFunction = { props.onDebugEnabled(!props.debugEnabled) } }
        }
        button(classes = "px-2 py-1 bg-e") {}
        div(classes = "flex-grow") {}
        syncIndicator {
          local = props.local
          remote = props.remote
          debugMode = props.debugEnabled // Hide the sync now button.
        }
      }
    }
