package party.markdown.ui.topBar

import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.readOnly
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import party.markdown.data.project.ProjectApi

@Composable
fun ProjectName(
    api: ProjectApi,
) {
  val name by api.currentName.collectAsState(initial = "")
  val scope = rememberCoroutineScope()
  var editedName by remember { mutableStateOf<String?>(null) }
  val editing = editedName != null

  Div(attrs = { classes("flex", "flex-row", "items-center", "space-x-4") }) {
    // The text field, which should not be editable by default.
    Input(
        InputType.Text,
        attrs = {
          classes("transition-all")
          classes("bg-gray-700", "border-gray-600", "hover:bg-gray-600", "text-white")
          classes("px-4", "py-2", "rounded")
          classes("text-lg", "font-medium")
          classes("appearance-none", "focus:outline-none", "focus:border-gray-400", "border-2")

          onClick { if (!editing) editedName = name }
          if (!editing) readOnly()
          value(editedName ?: name)
          onInput { event -> editedName = event.value }
        },
    )

    val src = if (editing) "/icons/project-name-save.svg" else "/icons/project-name-edit.svg"

    Button {
      Img(
          src = src,
          attrs = {
            classes("rounded-lg", "border-2", "border-gray-600", "hover:bg-gray-600", "p-3")
            onClick {
              val currentName = editedName
              if (currentName != null) {
                scope.launch {
                  api.name(currentName)
                  editedName = null
                }
              } else {
                editedName = name
              }
            }
          })
    }
  }
}
