package io.github.alexandrepiveteau.echo.core.buffer

/**
 * An interface representing a mutable gap buffer, containing a sequence of [Byte], which is
 * optimized for chunk insertions and removals.
 *
 * Gap buffers are data structures which support amortized constant-time consecutive insertions,
 * similarly to an array-based list, but at arbitrary buffer positions. They may be used to store a
 * sequence of items which are known to be inserted group-wise.
 *
 * In a gap buffer, positions are known as offsets. An offset is semantically identical to an index
 * in an array, except that it jumps over the gap.
 */
interface MutableByteGapBuffer {

  /** The backing [ByteArray]. */
  val backing: ByteArray

  /** How many items there are in the gap buffer. */
  val size: Int

  /** Some meta-data about the [Gap]. This may be useful for specific optimizations. */
  val gap: Gap

  /**
   * Gets the [Byte] at the given [offset].
   *
   * @throws IllegalArgumentException if the [offset] is out of bounds.
   */
  operator fun get(
      offset: Int,
  ): Byte

  /**
   * Sets the [Byte] at the given [offset].
   *
   * @throws IllegalArgumentException if the [offset] is out of bounds.
   */
  operator fun set(
      offset: Int,
      value: Byte,
  )

  /**
   * Pushes the given [Byte] at the provided offset (defaults to the end of the buffer).
   *
   * This operation may move the gap.
   */
  fun push(
      value: Byte,
      offset: Int = size,
  )

  /**
   * Pushes the given [ByteArray] at the provided offset (defaults to the end of the buffer).
   *
   * This operation may move the gap.
   */
  fun push(
      array: ByteArray,
      offset: Int = size,
      startIndex: Int = 0,
      endIndex: Int = array.size,
  )

  /**
   * Copies from the gap buffer into the provided [ByteArray].
   *
   * @param array the [ByteArray] into which data should be copied.
   * @param destinationOffset the destination index at which the copy starts.
   * @param startOffset the offset at which copy starts, in the gap buffer.
   *
   * This operation may move the gap.
   */
  fun copyInto(
      array: ByteArray,
      destinationOffset: Int = 0,
      startOffset: Int = 0,
      endOffset: Int = size,
  ): ByteArray

  /**
   * Removes the given count of items from the gap buffer at the given offset.
   *
   * This operation may move the gap.
   */
  fun remove(
      offset: Int,
      size: Int = 1,
  ): ByteArray

  /**
   * Removes the whole gap buffer, clearing the current data. This operation takes a constant time
   * and does not require moving the gap.
   */
  fun clear()
}
