package io.github.alexandrepiveteau.echo.core.buffer

/** Creates an empty [MutableGapBuffer]. */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> mutableGapBufferOf(): MutableGapBuffer<T> =
    MutableGapBufferImpl(arrayOfNulls<T>(Gap.DefaultSize) as Array<T>)

/**
 * Creates a [MutableGapBuffer] with a given [size], where each item is [init].
 *
 * @throws IllegalArgumentException if the size is negative.
 */
inline fun <reified T> MutableGapBuffer(
    size: Int,
    crossinline init: (Int) -> T,
): MutableGapBuffer<T> {
  require(size >= 0) { "MutableGapBuffer requires a positive size." }
  return mutableGapBufferOf<T>().apply { repeat(size) { push(init(it)) } }
}

/** Copies the contents of this [MutableGapBuffer] into a new [ByteArray]. */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> MutableGapBuffer<T>.toTypedArray(): Array<T> {
  return copyInto(arrayOfNulls<T>(size) as Array<T>)
}

/** Copies the contents of this [Array] into a new [MutableGapBuffer]. */
inline fun <reified T> Array<T>.toMutableGapBuffer(): MutableGapBuffer<T> {
  return MutableGapBuffer(size, this::get)
}
