package codemirror.state

/** See `https://codemirror.net/6/docs/ref/#state.Annotation`. */
@JsNonModule
@JsModule("@codemirror/state")
external class Annotation<T> {
  val type: AnnotationType<T>
  val value: T
  companion object {
    fun <T> define(): AnnotationType<T>
  }
}
