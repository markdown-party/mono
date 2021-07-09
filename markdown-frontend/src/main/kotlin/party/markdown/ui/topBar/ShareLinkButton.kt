package party.markdown.ui.topBar

import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.html.js.onClickFunction
import party.markdown.react.useLaunchedEffect
import react.*
import react.dom.button
import react.dom.img
import react.dom.span

fun RBuilder.buttonShareLink(
    block: ShareLinkButtonProps.() -> Unit,
): ReactElement = child(component) { attrs(block) }

external interface ShareLinkButtonProps : RProps {
  var publicLink: String
}

private val component =
    functionalComponent<ShareLinkButtonProps> { props ->
      val (copied, setCopied) = useState(false)

      useLaunchedEffect(listOf(copied)) {
        if (copied) {
          delay(2000)
          setCopied(false)
        }
      }

      val (text, icon) =
          when (copied) {
            true -> "Copied to clipboard" to "/icons/share-done.svg"
            false -> "Share link" to "/icons/share-copy.svg"
          }

      button(
          classes =
              """flex flex-row items-center
                 px-6 py-3 space-x-4
                 bg-gray-700 hover:bg-gray-500 transition
                 shadow hover:shadow-lg
                 rounded-lg
                 """,
      ) {
        attrs {
          onClickFunction =
              {
                setCopied(true)
                window.navigator.clipboard.writeText(props.publicLink)
              }
        }
        img(src = icon) {}
        span("uppercase") { +text }
      }
    }
