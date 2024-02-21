@file:JsNonModule
@file:JsModule("@codemirror/tooltip")

package codemirror.tooltip

import codemirror.view.EditorView

/**
 * Describes a tooltip. Values of this type, when provided through the showTooltip facet, control
 * the individual tooltips on the editor.
 */
external interface Tooltip {

  /** The document position at which to show the tooltip. */
  var pos: Int

  /** The end of the range annotated by this tooltip, if different from pos. */
  var end: Int

  /** A constructor function that creates the tooltip's DOM representation. */
  fun create(view: EditorView) : TooltipView

  /**
   * Whether the tooltip should be shown above or below the target position. Not guaranteed for
   * hover tooltips since all hover tooltips for the same range are always positioned together.
   * Defaults to false.
   */
  var above: Boolean

  /**
   * Whether the above option should be honored when there isn't enough space on that side to show
   * the tooltip inside the viewport. Not guaranteed for hover tooltips. Defaults to false.
   */
  var strictSide: Boolean
}
