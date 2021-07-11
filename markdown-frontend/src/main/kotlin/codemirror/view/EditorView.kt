@file:JsNonModule
@file:JsModule("@codemirror/view")

package codemirror.view

import codemirror.state.EditorState
import codemirror.state.Transaction
import codemirror.state.TransactionSpec
import org.w3c.dom.HTMLElement

/** See `https://codemirror.net/6/docs/ref/#view.EditorView`. */
external class EditorView
constructor(
    config: EditorViewConfig = definedExternally,
) {
  val state: EditorState
  val inView: Boolean
  val composing: Boolean
  val dom: HTMLElement
  val scrollDOM: HTMLElement
  val contentDOM: HTMLElement

  fun dispatch(tr: Transaction)
  fun dispatch(vararg specs: TransactionSpec)
  fun setState(newState: EditorState)
}
