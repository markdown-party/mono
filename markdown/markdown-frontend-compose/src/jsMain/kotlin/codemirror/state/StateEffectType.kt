package codemirror.state

/**
 * Representation of a type of state effect. Defined with [StateEffect.define].
 *
 * See `https://codemirror.net/6/docs/ref/#state.StateEffectType`.
 */
@JsNonModule
@JsModule("@codemirror/state")
external class StateEffectType<Value> {

  /** Create a [StateEffect] instance of this type. */
  fun of(value: Value): StateEffect<Value>
}
