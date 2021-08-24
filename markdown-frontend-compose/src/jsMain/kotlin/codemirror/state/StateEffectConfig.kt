package codemirror.state

external interface StateEffectConfig<Value> {

  /**
   * Provides a way to map an effect like this through a position mapping. When not given, the
   * effects will simply not be mapped. When the function returns undefined, that means the mapping
   * deletes the effect.
   */
  var map: (value: Value, mapping: ChangeDesc) -> Value
}

fun <Value> StateEffectConfig(): StateEffectConfig<Value> {
  return js("{}").unsafeCast<StateEffectConfig<Value>>()
}

fun <Value> StateEffectConfig(
    map: (value: Value, mapping: ChangeDesc) -> Value
): StateEffectConfig<Value> {
  val config = js("{}").unsafeCast<StateEffectConfig<Value>>()
  config.map = map
  return config
}
