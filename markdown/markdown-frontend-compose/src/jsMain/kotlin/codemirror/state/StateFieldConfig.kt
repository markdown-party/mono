package codemirror.state

/** @param Value the type of the values stored in the associated [StateField]. */
@JsNonModule
@JsModule("@codemirror/state")
external interface StateFieldConfig<Value> {

  /** Creates the initial value for the field when a state is created. */
  fun create(state: EditorState): Value

  /** Compute a new value from the field's previous value and a [Transaction]. */
  fun update(value: Value, transaction: Transaction): Value

  /**
   * Provide values for facets based on the value of this field. The given function will be called
   * once with the initialized field. It will usually want to call some facet's [Facet.from] method
   * to create facet inputs from this field, but can also return other extensions that should be
   * enabled by this field.
   */
  var provide: (field: StateField<Value>) -> Extension
}

fun <Value> StateFieldConfig(
    create: (state: EditorState) -> Value,
    update: (value: Value, transaction: Transaction) -> Value,
    provide: ((field: StateField<Value>) -> Extension)? = null,
): StateFieldConfig<Value> {
  val value = js("{}")
  value.create = create
  value.update = update
  if (provide != null) value.provide = provide
  return value.unsafeCast<StateFieldConfig<Value>>()
}
