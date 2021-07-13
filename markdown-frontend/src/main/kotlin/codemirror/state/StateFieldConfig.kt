package codemirror.state

/** @param Value the type of the values stored in the associated [StateField]. */
@JsNonModule
@JsModule("@codemirror/state")
external interface StateFieldConfig<Value> {

  /** Creates the initial value for the field when a state is created. */
  fun create(state: EditorState): Value

  /** Compute a new value from the field's previous value and a [Transaction]. */
  fun update(value: Value, transaction: Transaction): Value
}

fun <Value> StateFieldConfig(
    create: (state: EditorState) -> Value,
    update: (value: Value, transaction: Transaction) -> Value
): StateFieldConfig<Value> {
  val value = js("{}")
  value.create = create
  value.update = update
  return value.unsafeCast<StateFieldConfig<Value>>()
}
