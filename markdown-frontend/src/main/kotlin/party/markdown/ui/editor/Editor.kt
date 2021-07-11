package party.markdown.ui.editor

import codemirror.basicSetup.basicSetup
import codemirror.state.EditorState
import codemirror.state.EditorStateConfig
import codemirror.text.Text
import codemirror.view.EditorView
import codemirror.view.EditorViewConfig
import org.w3c.dom.HTMLElement
import react.*
import react.dom.div

fun RBuilder.editor(
    block: EditorProps.() -> Unit,
): ReactElement = child(editor) { attrs(block) }

external interface EditorProps : RProps

private val editor =
    functionalComponent<EditorProps> {
      val myRef = useRef<HTMLElement>()

      // Prepare the editor as an effect.
      // TODO : Somehow provide the state based on the current MutableSite value.
      useEffectWithCleanup(emptyList()) {
        val config = EditorViewConfig {
          state =
              EditorState.create(
                  EditorStateConfig {
                    doc = Text.of(arrayOf("Wait, collaboration doesn't work yet..."))
                    extensions = arrayOf(basicSetup)
                  })
          parent = myRef.current!!
        }
        EditorView(config)
        return@useEffectWithCleanup {}
      }

      div(classes = "flex-grow h-full") {
        attrs { ref { myRef.current = it.unsafeCast<HTMLElement>() } }
      }
    }
