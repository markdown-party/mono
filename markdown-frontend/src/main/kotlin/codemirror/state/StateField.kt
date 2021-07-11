package codemirror.state

/** See `https://codemirror.net/6/docs/ref/#state.StateField`. */
@JsNonModule
@JsModule("@codemirror/state")
external class StateField<Value> {
  fun init(create: (state: EditorState) -> Value): Extension
  val extension: Extension
}
