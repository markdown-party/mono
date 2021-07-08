package party.markdown.ui

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import party.markdown.data.tree.MutableSiteTreeApi
import party.markdown.data.tree.TreeApi
import party.markdown.tree.TreeEvent
import party.markdown.tree.TreeNode
import party.markdown.ui.debug.debugBar
import react.*
import react.dom.div

external interface MarkdownPartyProps : RProps {
  var local: MutableSite<TreeEvent, TreeNode>
  var remote: Exchange<Incoming, Outgoing>
}

private val app =
    functionalComponent<MarkdownPartyProps> { props ->

      // TODO : Inject this ?
      val api = useMemo<TreeApi>(listOf(props.local)) { MutableSiteTreeApi(props.local) }

      div(classes = "flex flex-col h-screen w-screen") {
        debugBar {
          local = props.local
          remote = props.remote
        }
        panes { treeApi = api }
      }
    }

fun RBuilder.markdownParty(
    block: MarkdownPartyProps.() -> Unit,
): ReactElement = child(app) { attrs(block) }