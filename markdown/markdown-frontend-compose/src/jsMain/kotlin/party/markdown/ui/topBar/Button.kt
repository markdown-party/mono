package party.markdown.ui.topBar

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Button as DomButton
import org.jetbrains.compose.web.dom.ContentBuilder
import org.w3c.dom.HTMLButtonElement

/**
 * Builds a styled [Button], which supports an [onClick] lambda that will be called when the user
 * presses the [Button].
 *
 * @param attrs the [AttrBuilderContext] for this button.
 * @param content the [ContentBuilder] for this button.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    attrs: AttrBuilderContext<HTMLButtonElement>? = null,
    content: ContentBuilder<HTMLButtonElement>? = null,
) {
  DomButton(
      attrs = {
        classes("flex", "flex-row", "items-center")
        classes("px-6", "py-3", "space-x-4")
        classes("text-white")
        classes("bg-blue-500", "hover:bg-blue-600", "transition")
        classes("shadow", "hover:shadow-lg")
        classes("rounded-lg")
        classes("uppercase")
        onClick { onClick() }
        if (attrs != null) attrs()
      },
      content = content,
  )
}
