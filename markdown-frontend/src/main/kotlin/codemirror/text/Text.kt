@file:JsNonModule
@file:JsModule("@codemirror/text")

package codemirror.text

/** See `https://codemirror.net/6/docs/ref/#text.Text`. */
abstract external class Text {
  abstract val length: Int
  abstract val lines: Int
  // TODO : () -> Iterator<string>
  fun lineAt(pos: Int): Line
  fun line(n: Int): Line
  fun replace(from: Int, to: Int, text: Text): Text
  fun append(other: Text): Text
  fun slice(from: Int, to: Int = definedExternally): Text
  abstract fun sliceString(
      from: Int,
      to: Int = definedExternally,
      lineSep: String = definedExternally,
  )
  fun eq(other: Text): Boolean
  fun iter(dir: Int = definedExternally): TextIterator
  fun iterRange(from: Int, to: Int = definedExternally): TextIterator
  fun toJSON(): Array<String>

  companion object {
    fun of(text: Array<String>): Text
    val empty: Text
  }
}
