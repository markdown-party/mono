@file:JsNonModule
@file:JsModule("@codemirror/state")

package codemirror.state

/** See `https://codemirror.net/6/docs/ref/#state.Annotation`. */
external class Annotation<T> {
  val type: AnnotationType<T>
  val value: T
  companion object {
    fun <T> define(): AnnotationType<T>
  }
}
