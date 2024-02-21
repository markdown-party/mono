package party.markdown.ui.navigator

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img

@Composable
private fun Icon(
    src: String,
    alt: String,
    onClick: () -> Unit,
) {
  Img(
      src = src,
      alt = alt,
      attrs = {
        classes("cursor-pointer")
        onClick { onClick() }
      },
  )
}

/**
 * Displays the actions that are available in the navigator. These actions let the user create some
 * new Markdown files, some new folders, as well as remove some content they might have already
 * added.
 */
@Composable
fun NavigatorActions(
    onClickNewMarkdownFile: () -> Unit,
    onClickNewFolder: () -> Unit,
) {
  Div(attrs = { classes("flex", "flex-row", "items-center", "space-x-4", "p-4", "bg-gray-200") }) {
    Icon(src = "/icons/navigator-action-new-text.svg", "New file", onClickNewMarkdownFile)
    Icon(src = "/icons/navigator-action-new-folder.svg", "New folder", onClickNewFolder)
    Div(attrs = { classes("flex-grow") })
  }
}
