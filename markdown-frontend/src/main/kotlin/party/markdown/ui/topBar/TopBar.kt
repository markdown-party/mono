package party.markdown.ui.topBar

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.protocol.Message
import kotlinx.browser.window
import kotlinx.html.js.onClickFunction
import party.markdown.MarkdownParty
import party.markdown.MarkdownPartyEvent
import react.*
import react.dom.button
import react.dom.div
import react.dom.img
import react.dom.span

fun RBuilder.topBar(
    block: TopBarProps.() -> Unit,
): ReactElement = child(component) { attrs(block) }

external interface TopBarProps : RProps {

  /** The public link that should be used to access the document from a separate browser. */
  var publicLink: String

  /**
   * The local [MutableSite], aka the local source of truth on which the operations are performed.
   */
  var local: MutableSite<MarkdownPartyEvent, MarkdownParty>

  /** The remote [Exchange], with which we should sync. */
  var remote: Exchange<Message.Incoming, Message.Outgoing>

  /** `true` if the debug options should be displayed. */
  var debugEnabled: Boolean

  /** A callback called whenever we should change the debug state. */
  var onDebugEnabled: (Boolean) -> Unit
}

private val component =
    functionalComponent<TopBarProps> { props ->
      div("p-4 space-x-4 bg-gray-800 text-white flex flex-row items-center") {
        img(
            src = "/img/logo.svg",
            classes = "h-12 cursor-pointer hover:bg-gray-600 rounded px-2 py-1") {
          attrs { onClickFunction = { props.onDebugEnabled(!props.debugEnabled) } }
        }
        div(classes = "flex-grow") {}
        button(
            classes =
                """flex flex-row items-center
                 px-6 py-3 space-x-4
                 bg-gray-700 hover:bg-gray-500 transition
                 shadow hover:shadow-lg 
                 rounded-lg
                 """,
        ) {
          attrs { onClickFunction = { window.open(window.location.origin) } }
          img(src = "/icons/new-project.svg") {}
          span("uppercase") { +"New project" }
        }
        buttonShareLink { publicLink = props.publicLink }
        syncIndicator {
          local = props.local
          remote = props.remote
          debugMode = props.debugEnabled // Hide the sync now button.
        }
      }
    }
