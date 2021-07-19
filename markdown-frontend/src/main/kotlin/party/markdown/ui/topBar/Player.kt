package party.markdown.ui.topBar

import react.*
import react.dom.div
import react.dom.span

/** Creates a new [ReactElement] using the [block] builder of [PlayerProps]. */
fun RBuilder.player(
    block: PlayerProps.() -> Unit,
): ReactElement = child(component) { attrs(block) }

/**
 * Some [RProps] that define how the current user should be displayed. The icon consists of a single
 * emoji, whereas the color will simply be applied as a CSS property.
 *
 * The player icon and colors will not be displayed in the text editor, but remain visible to the
 * user, so they can tell others who they are.
 */
external interface PlayerProps : RProps {

  /** The TailwindCSS color code for the player icon. */
  var color: String

  /** The player emoji. */
  var icon: String
}

private val component =
    functionalComponent<PlayerProps> { props ->
      div(
          classes =
              """
              m-2.5 h-14 w-14
              shadow-md rounded-full
              text-4xl
              flex flex-row justify-center items-center
              ${props.color}
          """) {
        span("text-2xl") { +props.icon }
      }
    }
