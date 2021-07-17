@file:JsNonModule
@file:JsModule("@codemirror/view")

package codemirror.view

import codemirror.state.EditorState
import codemirror.state.Extension
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

  /**
   * Update the view for the given [Array] of [Transaction]. This will update the visible document
   * and selection to match the state produced by the transactions, and notify view plugins of the
   * change. You should usually call [EditorView.dispatch] instead, which uses this as a primitive.
   */
  fun update(transactions: Array<Transaction>)

  fun setState(newState: EditorState)

  /**
   * Clean up this editor view, removing its element from the document, unregistering event
   * handlers, and notifying plugins. The view instance can no longer be used after calling this.
   */
  fun destroy()

  companion object {

    /**
     * An extension that enables line wrapping in the editor (by setting CSS white-space to pre-wrap
     * in the content).
     */
    val lineWrapping: Extension
  }
}
