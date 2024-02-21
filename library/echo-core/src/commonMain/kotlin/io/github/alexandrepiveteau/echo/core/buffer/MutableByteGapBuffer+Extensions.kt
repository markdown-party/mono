package io.github.alexandrepiveteau.echo.core.buffer

/** Creates an empty [MutableByteGapBuffer]. */
public fun mutableByteGapBufferOf(): MutableByteGapBuffer = MutableByteGapBufferImpl()

/**
 * Creates a [MutableByteGapBuffer] with a given [size], where each cell is init at zero.
 *
 * @throws IllegalArgumentException if the size is negative.
 */
public fun MutableByteGapBuffer(size: Int): MutableByteGapBuffer {
  return MutableByteGapBuffer(size) { 0 }
}

/**
 * Creates a [MutableByteGapBuffer] with a given [size], where each item is [init].
 *
 * @throws IllegalArgumentException if the size is negative.
 */
public fun MutableByteGapBuffer(size: Int, init: (Int) -> Byte): MutableByteGapBuffer {
  require(size >= 0) { "MutableByteGapBuffer requires a positive size." }
  return mutableByteGapBufferOf().apply { repeat(size) { push(init(it)) } }
}

/** Copies the contents of this [MutableByteGapBuffer] into a new [ByteArray]. */
public fun MutableByteGapBuffer.toByteArray(): ByteArray {
  return copyInto(ByteArray(size))
}

/** Copies the contents of this [MutableByteGapBuffer] into a new [Array] of [Byte]. */
public fun MutableByteGapBuffer.toTypedArray(): Array<Byte> {
  return toByteArray().toTypedArray()
}

/** Copies the contents of this [ByteArray] into a new [MutableByteGapBuffer]. */
public fun ByteArray.toMutableGapBuffer(): MutableByteGapBuffer {
  return MutableByteGapBuffer(size, this::get)
}

/** Copies the given range from the [MutableByteGapBuffer]. */
public fun MutableByteGapBuffer.copyOfRange(from: Int, until: Int): ByteArray {
  val size = until - from
  require(size >= 0) { "Can't copy a negative range." }
  return copyInto(ByteArray(size), startOffset = from, endOffset = until)
}
