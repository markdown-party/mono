@file:JsNonModule
@file:JsModule("@codemirror/state")

package codemirror.state

/** See `https://codemirror.net/6/docs/ref/#state.StateField`. */
external class StateField<Value> {

  /**
   * Returns an extension that enables this field and overrides the way it is initialized. Can be
   * useful when you need to provide a non-default starting value for the field.
   */
  fun init(create: (state: EditorState) -> Value): Extension

  /**
   * State field instances can be used as [Extension] values to enable the field in a given state.
   */
  val extension: Extension

  companion object {

    /** Define a state field. */
    fun <Value> define(config: StateFieldConfig<Value>): StateField<Value>
  }
}
