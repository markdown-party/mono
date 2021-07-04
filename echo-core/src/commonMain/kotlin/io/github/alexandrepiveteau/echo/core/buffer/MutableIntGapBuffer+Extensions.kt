package io.github.alexandrepiveteau.echo.core.buffer

/** Creates an empty [MutableIntGapBuffer]. */
fun mutableIntGapBufferOf(): MutableIntGapBuffer = MutableIntGapBufferImpl()

/**
 * Creates a [MutableIntGapBuffer] with a given [size], where each cell is init at zero.
 *
 * @throws IllegalArgumentException if the size is negative.
 */
fun MutableIntGapBuffer(size: Int): MutableIntGapBuffer {
  return MutableIntGapBuffer(size) { 0 }
}

/**
 * Creates a [MutableIntGapBuffer] with a given [size], where each item is [init].
 *
 * @throws IllegalArgumentException if the size is negative.
 */
fun MutableIntGapBuffer(size: Int, init: (Int) -> Int): MutableIntGapBuffer {
  require(size >= 0) { "MutableIntGapBuffer requires a positive size." }
  return mutableIntGapBufferOf().apply { repeat(size) { push(init(it)) } }
}

/** Copies the contents of this [MutableIntGapBuffer] into a new [IntArray]. */
fun MutableIntGapBuffer.toIntArray(): IntArray {
  return copyInto(IntArray(size))
}

/** Copies the contents of this [MutableIntGapBuffer] into a new [Array] of [Int]. */
fun MutableIntGapBuffer.toTypedArray(): Array<Int> {
  return toIntArray().toTypedArray()
}

/** Copies the contents of this [IntArray] into a new [MutableIntGapBuffer]. */
fun IntArray.toMutableGapBuffer(): MutableIntGapBuffer {
  return MutableIntGapBuffer(size, this::get)
}
