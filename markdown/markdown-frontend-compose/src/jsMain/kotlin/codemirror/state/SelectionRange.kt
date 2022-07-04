package codemirror.state

/**
 * A single selection range. When allowMultipleSelections is enabled, a selection may hold multiple
 * ranges. By default, selections hold exactly one range.
 *
 * This class provides some bindings to `https://codemirror.net/6/docs/ref/#state.SelectionRange`.
 */
@JsNonModule
@JsModule("@codemirror/state")
external class SelectionRange {

  /** The lower boundary of the range. */
  val from: Int

  /** The upper boundary of the range. */
  val to: Int

  /** The anchor of the range—the side that doesn't move when you extend it. */
  val anchor: Int

  /** The head of the range, which is moved when the range is extended. */
  val head: Int

  /** True when anchor and head are at the same position. */
  val empty: Boolean

  /**
   * If this is a cursor that is explicitly associated with the character on one of its sides, this
   * returns the side. -1 means the character before its position, 1 the character after, and 0
   * means no association.
   */
  val assoc: Int

  /** Compare this range to another range. */
  fun eq(other: SelectionRange): Boolean


}
