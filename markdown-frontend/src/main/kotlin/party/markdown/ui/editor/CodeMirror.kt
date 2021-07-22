package party.markdown.ui.editor

import codemirror.state.EditorState
import codemirror.state.EditorStateConfig
import codemirror.state.Extension
import codemirror.text.Text
import codemirror.view.EditorView
import codemirror.view.EditorViewConfig
import org.w3c.dom.HTMLElement
import react.*
import react.dom.div

fun RBuilder.codeMirror(
    block: CodeMirrorProps.() -> Unit,
): ReactElement = child(component) { attrs(block) }

external interface CodeMirrorProps : RProps {

  var id: Any?

  /** The [Extension]s that should be run when the CodeMirror editor is started. */
  var extensions: Array<Extension>

  /**
   * A [RMutableRef] to the current [EditorView]. This will be populated by the `CodeMirror`
   * component, whenever it will have started rendering on the screen.
   */
  var view: RMutableRef<EditorView>
}

// COMPONENT

private fun state(extensions: Array<Extension>): EditorState {
  return EditorState.create(
      EditorStateConfig {
        this.doc = Text.empty
        this.extensions = extensions
      },
  )
}

private val component =
    functionalComponent<CodeMirrorProps> { props ->
      val component = useRef<HTMLElement>()
      // TODO : Move the classes out of this component.
      div("flex-grow h-full min-w-0 max-h-full") { ref = component }

      // Whenever the component is available, run the effect and update the editor from the props.
      useEffectWithCleanup(listOf(props.id, component.current)) {
        if (component.current == null || props.id == null) return@useEffectWithCleanup {}

        lateinit var view: EditorView
        val config = EditorViewConfig {
          state = state(props.extensions)
          parent = component.current!!
        }
        view = EditorView(config)
        props.view.current = view
        return@useEffectWithCleanup {
          view.destroy()
          props.view.current = null
        }
      }
    }
