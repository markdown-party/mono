package codemirror.state

import codemirror.text.Text

/** See `https://codemirror.net/6/docs/ref/#state.EditorStateConfig`. */
@JsNonModule
@JsModule("@codemirror/state")
external interface EditorStateConfig {
  var doc: Text
  var extensions: Array<Extension>
}

/**
 * Creates a new empty [EditorStateConfig], which may be used to configure the default state of a
 * new [EditorState].
 */
fun EditorStateConfig(block: EditorStateConfig.() -> Unit): EditorStateConfig {
  val scope = js("{}").unsafeCast<EditorStateConfig>()
  block(scope)
  return scope
}
