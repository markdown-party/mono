package party.markdown.ui

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import kotlinx.html.js.onClickFunction
import party.markdown.react.useFlow
import party.markdown.react.useSync
import party.markdown.tree.TreeEvent
import party.markdown.tree.TreeNode
import party.markdown.ui.navigator.navigator
import react.*
import react.dom.button

external interface MarkdownPartyProps : RProps {
  var local: MutableSite<TreeEvent, TreeNode>
  var remote: Exchange<Incoming, Outgoing>
}

private val app =
    functionalComponent<MarkdownPartyProps> { props ->
      val tree = useFlow(props.local.value)
      val (syncing, requestSync) = useSync(props.local, props.remote)

      +"Syncing $syncing"
      button {
        attrs { onClickFunction = { requestSync() } }
        +"Sync now"
      }
      navigator { this.tree = tree }
    }

fun RBuilder.markdownParty(
    block: MarkdownPartyProps.() -> Unit,
): ReactElement = child(app) { attrs(block) }
