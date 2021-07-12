package party.markdown.ui.editor

import codemirror.basicSetup.basicSetup
import codemirror.lang.markdown.markdown
import codemirror.state.EditorState
import codemirror.state.EditorStateConfig
import codemirror.text.Text
import codemirror.view.EditorView
import codemirror.view.EditorViewConfig
import org.w3c.dom.HTMLElement
import party.markdown.data.text.TextApi
import party.markdown.tree.TreeNode
import react.*
import react.dom.div

fun RBuilder.editor(
    block: EditorProps.() -> Unit,
): ReactElement = child(editor) { attrs(block) }

external interface EditorProps : RProps {
  var node: TreeNode?
  var api: TextApi
}

private val EmptyEditorState =
    EditorState.create(
        EditorStateConfig {
          doc = Text.empty
          extensions = arrayOf(basicSetup, markdown())
        },
    )

private val editor =
    functionalComponent<EditorProps> { props ->
      val myRef = useRef<HTMLElement>()
      val id = props.node?.id

      // Prepare the editor as an effect.
      // TODO : Somehow provide the state based on the current MutableSite value.
      useEffectWithCleanup(listOf(id)) {
        lateinit var view: EditorView
        if (id != null) {
          val config = EditorViewConfig {
            state = EmptyEditorState
            dispatch =
                { tr ->
                  if (tr.docChanged) {
                    tr.changes.iterChanges(
                        f = { fA, tA, fB, tB, txt ->
                          println("fA: $fA, tA: $tA, fB: $fB, tB: $tB, txt: $txt")
                        },
                        individual = true,
                    )
                  }
                  view.update(arrayOf(tr))
                }
            parent = myRef.current!!
          }
          view = EditorView(config)
        }
        return@useEffectWithCleanup { if (id != null) view.destroy() }
      }

      div(classes = "flex-grow h-full min-w-0 max-h-full") {
        attrs { ref { myRef.current = it.unsafeCast<HTMLElement>() } }
      }
    }
