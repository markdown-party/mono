@file:JsNonModule
@file:JsModule("@codemirror/view")

package codemirror.view

import codemirror.state.EditorState
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
}
