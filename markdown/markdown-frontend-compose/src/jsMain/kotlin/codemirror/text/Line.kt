@file:JsNonModule
@file:JsModule("@codemirror/text")

package codemirror.text

/** See `https://codemirror.net/6/docs/ref/#text.Line`. */
external class Line {
  val from: Int
  val to: Int
  val number: Int
  val text: String
  val length: Int
}
