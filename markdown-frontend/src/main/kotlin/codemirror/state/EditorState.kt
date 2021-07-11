@file:JsNonModule
@file:JsModule("@codemirror/state")

package codemirror.state

import codemirror.text.Text

/** See `https://codemirror.net/6/docs/ref/#state.EditorState`. */
external class EditorState {
  val doc: Text
  fun <T> field(field: StateEffect<T>): T
  fun update(vararg specs: TransactionSpec): Transaction

  /** Get the value of a state [facet]. */
  fun <Output> facet(facet: Facet<dynamic, Output>): Output

  companion object {
    fun create(config: EditorStateConfig = definedExternally): EditorState

    /**
     * A facet that, when enabled, causes the editor to allow multiple ranges to be selected. Be
     * careful though, because by default the editor relies on the native DOM selection, which
     * cannot handle multiple selections. An extension like drawSelection can be used to make
     * secondary selections visible to the user.
     */
    val allowMultipleSelections: Facet<Boolean, Boolean>

    /**
     * Configures the tab size to use in this state. The first (highest-precedence) value of the
     * facet is used. If no value is given, this defaults to 4.
     */
    val tabSize: Facet<Int, Int>
  }
}
