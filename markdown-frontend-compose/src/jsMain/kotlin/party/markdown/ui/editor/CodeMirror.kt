package party.markdown.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import codemirror.state.EditorState
import codemirror.state.EditorStateConfig
import codemirror.state.Extension
import codemirror.text.Text
import codemirror.view.EditorView
import codemirror.view.EditorViewConfig
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

private fun state(extensions: Array<Extension>): EditorState {
  return EditorState.create(
      EditorStateConfig {
        this.doc = Text.empty
        this.extensions = extensions
      },
  )
}

@Composable
fun rememberEditorView(
    key1: Any?,
    extensions: Array<Extension>,
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
): State<EditorView?> {
  val view = remember(key1) { mutableStateOf<EditorView?>(null) }
  Div(attrs = attrs) {
    DisposableRefEffect(key1) { element ->
      val config = EditorViewConfig {
        state = state(extensions)
        parent = element
      }
      view.value = EditorView(config)
      onDispose {
        view.value?.destroy()
        view.value = null
      }
    }
  }
  return view
}
