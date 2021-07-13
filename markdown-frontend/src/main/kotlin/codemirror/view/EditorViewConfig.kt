package codemirror.view

import codemirror.state.EditorState
import codemirror.state.Transaction
import org.w3c.dom.Document
import org.w3c.dom.Element

@JsNonModule
@JsModule("@codemirror/view")
external interface EditorViewConfig {
  var state: EditorState
  var root: Document // or ShadowRoot

  /**
   * Override the transaction [EditorView.dispatch] function for this editor view, which is the way
   * updates get routed to the view. Your implementation, if provided, should probably call the
   * view's [EditorView.update] method.
   */
  var dispatch: (tr: Transaction) -> Unit

  var parent: Element // or DocumentFragment
}

fun EditorViewConfig(block: EditorViewConfig.() -> Unit): EditorViewConfig {
  val scope = js("{}").unsafeCast<EditorViewConfig>()
  block(scope)
  return scope
}
