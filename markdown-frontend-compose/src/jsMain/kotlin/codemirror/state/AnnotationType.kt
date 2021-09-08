package codemirror.state

/** See `https://codemirror.net/6/docs/ref/#state.AnnotationType`. */
@JsNonModule
@JsModule("@codemirror/state")
external class AnnotationType<T> {
  fun of(value: T): Annotation<T>
}
