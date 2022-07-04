@file:JsNonModule
@file:JsModule("@codemirror/text")

package codemirror.text

/** See `https://codemirror.net/6/docs/ref/#text.TextIterator`. */
external interface TextIterator {
  fun next(skip: Int = definedExternally): TextIterator
  val value: String
  val done: Boolean
  val lineBreak: Boolean
}
