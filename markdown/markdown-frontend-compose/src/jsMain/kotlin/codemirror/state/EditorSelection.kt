package codemirror.state

/**
 * An editor selection holds one or more selection ranges.
 *
 * This class provides some bindings to `https://codemirror.net/6/docs/ref/#state.EditorSelection`.
 */
@JsNonModule
@JsModule("@codemirror/state")
external class EditorSelection {

  /**
   * The ranges in the selection, sorted by position. Ranges cannot overlap (but they may touch, if
   * they aren't empty).
   */
  val ranges: Array<SelectionRange>

  /**
   * The index of the main range in the selection (which is usually the range that was added last).
   */
  val mainIndex: Int

  /** Compare this selection to another selection. */
  fun eq(other: EditorSelection): Boolean

  /**
   * Get the primary selection range. Usually, you should make sure your code applies to all ranges,
   * by using methods like changeByRange.
   */
  val main: SelectionRange

  /**
   * Make sure the selection only has one range. Returns a selection holding only the main range
   * from this selection.
   */
  fun asSingle(): EditorSelection

  /** Extend this selection with an extra range. */
  fun addRange(range: SelectionRange, main: Boolean = definedExternally): EditorSelection
}
