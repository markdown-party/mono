@file:JsNonModule
@file:JsModule("@codemirror/tooltip")

package codemirror.tooltip

import codemirror.view.EditorView
import org.w3c.dom.HTMLElement

/** Describes the way a tooltip is displayed */
external interface TooltipView {

  /** The DOM element to position over the editor. */
  var dom: HTMLElement

  /** Called after the tooltip is added to the DOM for the first time. */
  var mount: (view: EditorView) -> Unit

  // Update the DOM element for a change in the view's state.
  // val update: (update: ViewUpdate) -> Unit

  /** Called when the tooltip has been (re)positioned. */
  var positioned: () -> Unit
}
