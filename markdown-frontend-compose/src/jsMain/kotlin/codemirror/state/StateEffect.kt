@file:JsNonModule
@file:JsModule("@codemirror/state")

package codemirror.state

/**
 * State effects can be used to represent additional effects associated with a [Transaction]. They
 * are often useful to model changes to custom [StateField], when those changes aren't implicit in
 * document or selection changes.
 *
 * See `https://codemirror.net/6/docs/ref/#state.StateEffect`.
 */
external class StateEffect<Value> {

  /** The value of this effect. */
  val value: Value

  /**
   * Map this effect through a position mapping. Will return undefined when that ends up deleting
   * the effect.
   */
  fun map(mapping: ChangeDesc): StateEffect<Value>

  /** Tells you whether this effect object is of a given [StateEffectType]. */
  @JsName("is") fun <T> isInstance(type: StateEffectType<Value>): Boolean

  companion object {

    /**
     * Define a new effect type. The type parameter [Value] indicates the type of values that his
     * effect holds.
     */
    fun <Value> define(spec: StateEffectConfig<Value> = definedExternally): StateEffectType<Value>
  }
}
