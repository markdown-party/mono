package io.github.alexandrepiveteau.echo.core.buffer

import io.github.alexandrepiveteau.echo.core.requireIn

/**
 * An implementation of [MutableGapBuffer]. The class implements [Gap] to avoid additional
 * allocations when the buffer's [Gap] is fetched.
 *
 * This implementation is optimized to perform chunk array copies, rather than element-wise
 * operations on the underlying buffer.
 */
@PublishedApi
internal class MutableGapBufferImpl<T>(
    internal var buffer: Array<T>,
) : MutableGapBuffer<T>, Gap {

  override var startIndex: Int = 0
  override var endIndex: Int = Gap.DefaultSize

  // HELPERS

  /** Converts the given [Int] [offset] into the associated [buffer] index. */
  private fun offsetToIndex(offset: Int): Int {
    requireIn(offset, 0, this.size)
    return if (offset < this.startIndex) offset else offset + this.capacity
  }

  /**
   * Calculates the next size for the inner buffer, assuming the given [capacity] should be added.
   * If the current [capacity] is sufficient, the current backing [buffer] size will be returned.
   *
   * This should be used in conjunction with [grow]. Dissociating the [nextSize] and [grow] steps
   * avoid creating multiple backing buffers when a resize more than doubles the backing buffer
   * size.
   *
   * @throws IllegalArgumentException if the additional required capacity is negative
   */
  private fun nextSize(capacity: Int): Int {
    if (capacity < 0) throw IllegalArgumentException("Capacity must be strictly positive.")
    if (this.capacity >= capacity) return this.buffer.size
    var size = this.buffer.size
    while (size != Int.MAX_VALUE && size - this.buffer.size + this.capacity < capacity) {
      size *= 2
      // On overflow, the next size is Int.MAX_VALUE
      if (size < 0) size = Int.MAX_VALUE
    }
    return size
  }

  /**
   * Grows the [MutableGapBuffer] backing [buffer] to the given [size]. If the provided [size] is
   * smaller than the current backing [buffer] size, an exception will be thrown.
   *
   * @throws IllegalArgumentException if the backing size is would require shrinking.
   */
  @Suppress("UNCHECKED_CAST")
  private fun grow(size: Int) {
    // Preconditions.
    require(size >= this.buffer.size)

    // Optimizations.
    if (size == this.buffer.size) return // fast path

    val end = size - (this.buffer.size - this.endIndex)
    val buffer = this.buffer.copyOf(size)
    this.buffer.copyInto(buffer, 0, startIndex = 0, endIndex = this.startIndex)
    this.buffer.copyInto(buffer, end, startIndex = this.endIndex, endIndex = this.buffer.size)
    this.endIndex = end
    this.buffer = buffer as Array<T>
  }

  /** Moves the gap by an amount of one to the left, or no-ops if already at a boundary. */
  private fun left() {
    if (this.startIndex > 0) {
      this.startIndex--
      this.endIndex--
      this.buffer[this.endIndex] = this.buffer[this.startIndex]
    }
  }

  /** Moves the gap by an amount of one to the right, or no-ops if already at a boundary. */
  private fun right() {
    if (this.endIndex < this.buffer.size) {
      this.buffer[this.startIndex] = this.buffer[this.endIndex]
      this.startIndex++
      this.endIndex++
    }
  }

  // TODO : Chunk-based move.
  private fun move(to: Int) {
    val bounded = to.coerceIn(0, size)
    while (this.startIndex != bounded) {
      if (this.startIndex > bounded) left()
      if (this.startIndex < bounded) right()
    }
  }

  private val capacity: Int
    get() = this.endIndex - this.startIndex

  // IMPLEMENTATION - GAP

  override fun shift(
      amount: Int,
  ) {
    move(to = this.startIndex + amount)
  }

  // IMPLEMENTATION - BUFFER

  override val backing: Array<T>
    get() = buffer

  override val size: Int
    get() = this.buffer.size - capacity

  override val gap: Gap
    get() = this

  override fun get(offset: Int): T {
    // Preconditions.
    requireIn(offset, 0, this.size)

    // Convert the indices, without moving the gap.
    return this.buffer[offsetToIndex(offset)]
  }

  override fun set(offset: Int, value: T) {
    // Preconditions.
    requireIn(offset, 0, this.size)

    // Convert the indices, without moving the gap.
    this.buffer[offsetToIndex(offset)] = value
  }

  override fun push(value: T, offset: Int) {
    // Preconditions.
    requireIn(offset, 0, this.size + 1)

    // Grow the buffer by one, then move to the insertion index, and finally insert the new element
    // into the gap by incrementing the startIndex.
    //
    // The ordering of growing then moving matters.
    grow(nextSize(capacity = 1))
    move(to = offset)
    this.buffer[this.startIndex++] = value
  }

  override fun push(array: Array<T>, offset: Int, startIndex: Int, endIndex: Int) {
    // Range preconditions
    require(endIndex >= startIndex)
    requireIn(startIndex, 0, array.size + 1)
    requireIn(endIndex, 0, array.size + 1)
    requireIn(offset, 0, this.size + 1)

    // Optimizations.
    if (endIndex == startIndex) return

    // Grow the buffer by making sure we have enough capacity for the newly added elements. Then,
    // the cursor will be moved to the insertion index, and the input data will be copied starting
    // at the current cursor position. Finally, the startIndex is incremented.
    grow(nextSize(capacity = endIndex - startIndex))
    move(to = offset)
    array.copyInto(this.buffer, offset, startIndex, endIndex)
    this.startIndex += (endIndex - startIndex)
  }

  override fun copyInto(
      array: Array<T>,
      destinationOffset: Int,
      startOffset: Int,
      endOffset: Int
  ): Array<T> {
    // Preconditions.
    requireIn(destinationOffset, 0, array.size + 1)
    require(destinationOffset + endOffset - startOffset in 0..array.size)
    requireIn(startOffset, 0, this.size + 1)
    requireIn(endOffset, 0, this.size + 1)
    require(startOffset <= endOffset)

    // Optimizations.
    if (endOffset == startOffset) return array

    @Suppress("ConvertTwoComparisonsToRangeCheck", "SpellCheckingInspection")
    if (startOffset < startIndex && endOffset <= startIndex) {
      // The copy happens in the start buffer. Copy the head part.
      //
      // Example:
      // Input:
      //   buffer:      ABCDEFGHIJKLMNOPQ*******RSTUVWXYZ
      //   copy region:    |----|
      buffer.copyInto(
          destination = array,
          destinationOffset = destinationOffset,
          startIndex = startOffset,
          endIndex = endOffset,
      )
    } else if (startOffset < startIndex && startIndex <= endOffset) {
      // The copy happens across the buffer. Copy both the head and the tail part.
      //
      // Example:
      // Input:
      //   buffer:      ABCDEFGHIJKLMNOPQ*******RSTUVWXYZ
      //   copy region:               |------------|
      buffer.copyInto(
          destination = array,
          destinationOffset = destinationOffset,
          startIndex = startOffset,
          endIndex = startIndex,
      )
      buffer.copyInto(
          destination = array,
          destinationOffset = destinationOffset + (startIndex - startOffset),
          startIndex = endIndex,
          endIndex = endIndex + (endOffset - startOffset) - (startIndex - startOffset),
      )
    } else { // startOffset >= startIndex && endOffset >= startIndex
      // The copy happens at the end of the buffer. Copy the tail part.
      //
      // Example:
      // Input:
      //   buffer:      ABCDEFGHIJKLMNOPQ*******RSTUVWXYZ
      //   copy region:                          |-----|
      buffer.copyInto(
          destination = array,
          destinationOffset = destinationOffset,
          startIndex = startOffset + (endIndex - startIndex),
          endIndex = startOffset + (endOffset - startOffset) + (endIndex - startIndex),
      )
    }

    return array
  }

  @Suppress("UNCHECKED_CAST")
  override fun remove(offset: Int, size: Int) {
    // Preconditions.
    requireIn(offset, 0, this.size)
    requireIn(offset + size, 0, this.size + 1)
    move(to = offset)

    // Increment the endIndex, and then copy the previous size elements from the buffer into the
    // returned array. Because the cursor was moved, we can simply sequentially move them.
    this.endIndex += size
  }

  override fun clear() {
    // We can reset the buffer by having expanding the gap to the buffer boundaries.
    this.startIndex = 0
    this.endIndex = this.buffer.size
  }

  /**
   * Outputs a [String] representation of the [MutableGapBuffer]. Ts which are part of the gap will
   * be prefixed by a '*' symbol.
   */
  override fun toString(): String {
    val content = gap.bufferToString(buffer)
    return "MutableGapBuffer(start=$startIndex, end=$endIndex, data=[${content}])"
  }
}
