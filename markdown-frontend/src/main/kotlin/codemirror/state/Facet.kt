package codemirror.state

/**
 * A facet is a labeled value that is associated with an editor state. It takes inputs from any
 * number of extensions, and combines those into a single output value. Examples of facets are the
 * theme styles associated with an editor or the tab size (which is reduced to a single value, using
 * the input with the highest precedence).
 *
 * This class provides some bindings to `https://codemirror.net/6/docs/ref/#state.Facet`.
 */
@JsNonModule
@JsModule("@codemirror/state")
external class Facet<Input, Output> {

  /** Returns an extension that adds the given value for this facet. */
  fun of(value: Input): Extension

  /** Create an extension that computes zero or more values for this facet from a state. */
  fun computeN(deps: Array<dynamic>, get: (state: EditorState) -> Array<Input>): Extension

  /**
   * Shorthand method for registering a facet source with a state field as input. If the field's
   * type corresponds to this facet's input type, the getter function can be omitted. If given, it
   * will be used to retrieve the input from the field value.
   */
  fun from(field: StateField<Input>): Extension

  /**
   * Shorthand method for registering a facet source with a state field as input. If the field's
   * type corresponds to this facet's input type, the getter function can be omitted. If given, it
   * will be used to retrieve the input from the field value.
   */
  fun <T> from(field: StateField<T>, get: (value: T) -> Input): Extension

  companion object {

    /** Define a new facet. */
    fun <Input, Output> define(config: dynamic = definedExternally): Facet<Input, Output>
  }
}
