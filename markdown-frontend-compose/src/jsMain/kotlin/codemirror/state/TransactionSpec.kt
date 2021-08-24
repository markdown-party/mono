package codemirror.state

/** See `https://codemirror.net/6/docs/ref/#state.TransactionSpec`. */
@JsNonModule
@JsModule("@codemirror/state")
external interface TransactionSpec {

  /** The changes to the document made by this transaction. */
  @JsName("changes") var rawChanges: dynamic

  /**
   * When set, this transaction explicitly updates the selection. Offsets in this selection should
   * refer to the document as it is after the transaction.
   */
  var selection: EditorSelection

  @JsName("effects") var rawEffects: dynamic
  @JsName("annotations") var rawAnnotations: dynamic

  /**
   * When set to true, the transaction is marked as needing to scroll the current selection into
   * view.
   */
  var scrollIntoView: Boolean

  /**
   * By default, transactions can be modified by change filters and transaction filters. You can set
   * this to false to disable that.
   */
  var filter: Boolean

  /**
   * Normally, when multiple specs are combined (for example by [EditorState.update]), the positions
   * in changes are taken to refer to the document positions in the initial document. When a spec
   * has sequential set to true, its positions will be taken to refer to the document created by the
   * specs before it instead.
   */
  var sequential: Boolean
}

fun <T> TransactionSpec.effect(): StateEffect<T> {
  return rawEffects.unsafeCast<StateEffect<T>>()
}

fun <T> TransactionSpec.effects(): Array<StateEffect<T>> {
  return rawEffects.unsafeCast<Array<StateEffect<T>>>()
}

var TransactionSpec.annotation: Annotation<Any>
  get() = error("Unsupported operation.")
  set(value) {
    rawAnnotations = value
  }

var TransactionSpec.annotations: Array<Annotation<*>>
  get() = error("Unsupported operation.")
  set(value) {
    rawAnnotations = value
  }

fun TransactionSpec(
    block: TransactionSpec.() -> Unit,
): TransactionSpec = js("{}").unsafeCast<TransactionSpec>().apply(block)
