package party.markdown.ui.topBar

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Displays a current user who should be displayed. The icon consists of a single emoji, whereas the
 * color will simply be applied as a CSS property.
 *
 * The player icon and colors will not be displayed in the text editor, but remain visible to the
 * user, so they can tell others who they are.
 *
 * @param color the TailwindCSS color code for the player icon.
 * @param icon the player emoji.
 */
@Composable
fun Player(
    color: String,
    icon: String,
) {
  Div(
      attrs = {
        classes("m-2.5", "h-14", "w-14")
        classes("shadow-md", "rounded-full")
        classes("text-4xl")
        classes("flex", "flex-row", "justify-center", "items-center")
        classes(color)
      },
  ) { Span(attrs = { classes("text-2xl") }) { Text(icon) } }
}
