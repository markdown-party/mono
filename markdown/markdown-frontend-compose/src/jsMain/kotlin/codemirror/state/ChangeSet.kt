package codemirror.state

import codemirror.text.Text

/**
 * A change set represents a group of modifications to a document. It stores the document length,
 * and can only be applied to documents with exactly that length.
 */
@JsNonModule
@JsModule("@codemirror/state")
external class ChangeSet : ChangeDesc {

  /** Apply the changes to a document, returning the modified document. */
  fun apply(doc: Text): Text

  /**
   * Given the document as it existed before the changes, return a change set that represents the
   * inverse of this set, which could be used to go from the document created by the changes back to
   * the document as it existed before the changes.
   */
  fun invert(doc: Text): ChangeSet

  /**
   * Combine two subsequent change sets into a single set. [other] must start in the document
   * produced by this. If this goes docA → docB and other represents docB → docC, the returned value
   * will represent the change docA → docC.
   */
  fun compose(other: ChangeSet): ChangeSet

  /**
   * Given another change set starting in the same document, maps this change set over the other,
   * producing a new change set that can be applied to the document produced by applying other. When
   * before is true, order changes as if this comes before other, otherwise (the default) treat
   * other as coming first. Given two changes A and B, A.compose(B.map(A)) and B.compose(A.map(B,
   * true)) will produce the same document. This provides a basic form of operational
   * transformation, and can be used for collaborative editing.
   */
  fun map(other: ChangeDesc, before: Boolean = definedExternally): ChangeSet

  /**
   * Iterate over the changed ranges in the document, calling [f] for each. When [individual] is
   * `true`, adjacent changes are reported separately.
   */
  fun iterChanges(
      f: (fromA: Int, toA: Int, fromB: Int, toB: Int, inserted: Text) -> Unit,
      individual: Boolean = definedExternally,
  )

  /** Get a [ChangeDesc] for this change set. */
  val desc: ChangeDesc
}
