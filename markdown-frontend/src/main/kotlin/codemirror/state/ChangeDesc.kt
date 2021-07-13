package codemirror.state

/**
 * A change description is a variant of [ChangeSet] that doesn't store the inserted text. As such,
 * it can't be applied, but is cheaper to store and manipulate.
 */
@JsNonModule
@JsModule("@codemirror/state")
open external class ChangeDesc {

  /** The length of the document before the change. */
  val length: Int

  /** The length of the document after the change. */
  val newLength: Int

  /** False when there are actual changes in this set. */
  val empty: Boolean
}
