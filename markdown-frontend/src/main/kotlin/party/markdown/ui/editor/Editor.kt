package party.markdown.ui.editor

import codemirror.basicSetup.basicSetup
import codemirror.state.ChangeSpec
import codemirror.state.EditorState
import codemirror.state.EditorStateConfig
import codemirror.state.TransactionSpec
import codemirror.text.Text
import codemirror.view.EditorView
import codemirror.view.EditorViewConfig
import kotlinx.coroutines.delay
import org.w3c.dom.HTMLElement
import party.markdown.react.useLaunchedEffect
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
      useLaunchedEffect(emptyList()) {
        val config = EditorViewConfig {
          state =
              EditorState.create(
                  EditorStateConfig {
                    doc = Text.of(arrayOf("Wait, collaboration doesn't work yet..."))
                    extensions = arrayOf(basicSetup)
                  })
          parent = myRef.current!!
        }
        val view = EditorView(config)

        while (true) {
          delay(10 * 1000)
          val transaction =
              view.state.update(
                  TransactionSpec {
                    rawChanges =
                        ChangeSpec(
                            from = 0,
                            to = 0,
                            insert = Text.of(arrayOf("hi! ")),
                        )
                  })
          view.dispatch(transaction)
        }
      }

      div(classes = "flex-grow h-full") {
        attrs { ref { myRef.current = it.unsafeCast<HTMLElement>() } }
      }
    }
