package io.github.alexandrepiveteau.echo.core.buffer

/** Creates an empty [MutableByteGapBuffer]. */
fun mutableByteGapBufferOf(): MutableByteGapBuffer = MutableByteGapBufferImpl()

/**
 * Creates a [MutableByteGapBuffer] with a given [size], where each cell is init at zero.
 *
 * @throws IllegalArgumentException if the size is negative.
 */
fun MutableByteGapBuffer(size: Int): MutableByteGapBuffer {
  return MutableByteGapBuffer(size) { 0 }
}

/**
 * Creates a [MutableByteGapBuffer] with a given [size], where each item is [init].
 *
 * @throws IllegalArgumentException if the size is negative.
 */
fun MutableByteGapBuffer(size: Int, init: (Int) -> Byte): MutableByteGapBuffer {
  require(size >= 0) { "MutableByteGapBuffer requires a positive size." }
  return mutableByteGapBufferOf().apply { repeat(size) { push(init(it)) } }
}

/** Copies the contents of this [MutableByteGapBuffer] into a new [ByteArray]. */
fun MutableByteGapBuffer.toByteArray(): ByteArray {
  return copyInto(ByteArray(size))
}

/** Copies the contents of this [MutableByteGapBuffer] into a new [Array] of [Byte]. */
fun MutableByteGapBuffer.toTypedArray(): Array<Byte> {
  return toByteArray().toTypedArray()
}
