package party.markdown.ui.navigator

import kotlinx.html.js.onClickFunction
import react.*
import react.dom.div
import react.dom.img

/**
 * Displays the actions that are available in the navigator. These actions let the user create some
 * new Markdown files, some new folders, as well as remove some content they might have already
 * added.
 */
fun RBuilder.navigatorActions(
    block: NavigatorActionsProps.() -> Unit,
): ReactElement = child(navigatorActions) { attrs(block) }

external interface NavigatorActionsProps : RProps {
  var onClickNewMarkdownFile: () -> Unit
  var onClickNewFolder: () -> Unit
  var onClickDelete: () -> Unit
}

// COMPONENT

private fun RBuilder.icon(
    src: String,
    alt: String,
    onClick: () -> Unit,
): ReactElement =
    img(
        src = src,
        alt = alt,
        classes = "cursor-pointer",
    ) { attrs { onClickFunction = { onClick() } } }

private val navigatorActions =
    functionalComponent<NavigatorActionsProps> { props ->
      div("flex flex-row items-center space-x-4 p-4 bg-gray-800") {
        icon(src = "/icons/navigator-action-new-text.svg", "New file", props.onClickNewMarkdownFile)
        icon(src = "/icons/navigator-action-new-folder.svg", "New folder", props.onClickNewFolder)
        div("flex-grow") {}
        icon(src = "/icons/navigator-action-delete.svg", "Delete", props.onClickDelete)
      }
    }
