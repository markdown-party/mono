package io.github.alexandrepiveteau.echo.core.buffer

/** Creates an empty [MutableCharGapBuffer]. */
public fun mutableCharGapBufferOf(): MutableCharGapBuffer = MutableCharGapBufferImpl()

/**
 * Creates a [MutableCharGapBuffer] with a given [size], where each cell is init at zero.
 *
 * @throws IllegalArgumentException if the size is negative.
 */
public fun MutableCharGapBuffer(size: Int): MutableCharGapBuffer {
  return MutableCharGapBuffer(size) { Char.MIN_VALUE }
}

/**
 * Creates a [MutableCharGapBuffer] with a given [size], where each item is [init].
 *
 * @throws IllegalArgumentException if the size is negative.
 */
public fun MutableCharGapBuffer(size: Int, init: (Int) -> Char): MutableCharGapBuffer {
  require(size >= 0) { "MutableCharGapBuffer requires a positive size." }
  return mutableCharGapBufferOf().apply { repeat(size) { push(init(it)) } }
}

/** Copies the contents of this [MutableCharGapBuffer] into a new [CharArray]. */
public fun MutableCharGapBuffer.toCharArray(): CharArray {
  return copyInto(CharArray(size))
}

/** Copies the contents of this [MutableCharGapBuffer] into a new [Array] of [Char]. */
public fun MutableCharGapBuffer.toTypedArray(): Array<Char> {
  return toCharArray().toTypedArray()
}

/** Copies the contents of this [CharArray] into a new [MutableCharGapBuffer]. */
public fun CharArray.toMutableGapBuffer(): MutableCharGapBuffer {
  return MutableCharGapBuffer(size, this::get)
}

/** Copies the given range from the [MutableCharGapBuffer]. */
public fun MutableCharGapBuffer.copyOfRange(from: Int, until: Int): CharArray {
  val size = until - from
  require(size >= 0) { "Can't copy a negative range." }
  return copyInto(CharArray(size), startOffset = from, endOffset = until)
}
