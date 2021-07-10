package party.markdown.ui.topBar

import kotlinx.coroutines.launch
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onSubmitFunction
import party.markdown.data.project.ProjectApi
import party.markdown.react.useCoroutineScope
import party.markdown.react.useFlow
import react.*
import react.dom.button
import react.dom.div
import react.dom.img
import react.dom.input

fun RBuilder.projectName(
    block: ProjectNameProps.() -> Unit,
): ReactElement = child(component) { attrs(block) }

external interface ProjectNameProps : RProps {
  var api: ProjectApi
}

private val component =
    functionalComponent<ProjectNameProps> { props ->
      val name = useFlow(props.api.currentName)
      val scope = useCoroutineScope()

      val (editedName, setEditedName) = useState<String?>(null)
      val editing = editedName != null
      div("flex flex-row items-center space-x-4") {
        // The text field, which should not be editable by default.
        input(
            classes =
                """transition-all
                 bg-gray-700 border-gray-600 hover:bg-gray-600 text-white
                 px-4 py-2 rounded
                 text-lg font-medium
                 appearance-none focus:outline-none focus:border-gray-400 border-2
                 """,
        ) {
          attrs {
            this.onClickFunction = { if (!editing) setEditedName(name) }
            this.readonly = !editing
            this.value = editedName ?: name
            this.onChangeFunction = { setEditedName(it.target.asDynamic().value as? String) }
            this.onSubmitFunction =
                {
                  editedName?.let { name ->
                    scope.launch {
                      props.api.name(name)
                      setEditedName(null)
                    }
                  }
                }
          }
        }

        val src = if (editing) "/icons/project-name-save.svg" else "/icons/project-name-edit.svg"

        button {
          img(src = src, classes = "rounded-lg border-2 border-gray-600 hover:bg-gray-600 p-3") {
            attrs {
              onClickFunction =
                  {
                    if (editedName != null) {
                      scope.launch {
                        props.api.name(editedName)
                        setEditedName(null)
                      }
                    } else {
                      setEditedName(name)
                    }
                  }
            }
          }
        }
      }
    }
