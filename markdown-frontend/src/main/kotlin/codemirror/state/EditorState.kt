@file:JsNonModule
@file:JsModule("@codemirror/state")

package codemirror.state

import codemirror.text.Text

/** See `https://codemirror.net/6/docs/ref/#state.EditorState`. */
external class EditorState {
  val doc: Text

  companion object {
    fun create(config: EditorStateConfig = definedExternally): EditorState
  }
}
