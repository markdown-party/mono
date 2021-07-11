package codemirror.view

import codemirror.state.EditorState
import org.w3c.dom.Document
import org.w3c.dom.Element

@JsNonModule
@JsModule("@codemirror/view")
external interface EditorViewConfig {
  var state: EditorState
  var root: Document // or ShadowRoot
  var parent: Element // or DocumentFragment
}

fun EditorViewConfig(block: EditorViewConfig.() -> Unit): EditorViewConfig {
  val scope = js("{}").unsafeCast<EditorViewConfig>()
  block(scope)
  return scope
}
