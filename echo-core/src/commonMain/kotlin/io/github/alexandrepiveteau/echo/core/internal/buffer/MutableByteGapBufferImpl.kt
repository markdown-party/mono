package io.github.alexandrepiveteau.echo.core.internal.buffer

/**
 * An implementation of [MutableByteGapBuffer]. The class implements [Gap] to avoid additional
 * allocations when the buffer's [Gap] is fetched.
 *
 * This implementation is optimized to perform chunk array copies, rather than element-wise
 * operations on the underlying buffer.
 */
internal class MutableByteGapBufferImpl : MutableByteGapBuffer, Gap {

  override var startIndex: Int = 0
  override var endIndex: Int = Gap.DefaultSize

  // Internal visibility is defined for testing.
  internal var buffer = ByteArray(Gap.DefaultSize)

  // HELPERS

  /** Converts the given [Int] [offset] into the associated [buffer] index. */
  private fun offsetToIndex(offset: Int): Int {
    if (offset < 0 || offset >= this.size) throw IndexOutOfBoundsException()
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
   * Grows the [MutableByteGapBuffer] backing [buffer] to the given [size]. If the provided [size]
   * is smaller than the current backing [buffer] size, an exception will be thrown.
   *
   * @throws IllegalArgumentException if the backing size is would require shrinking.
   */
  private fun grow(size: Int) {
    // Preconditions.
    check(size >= this.buffer.size)

    // Optimizations.
    if (size == this.buffer.size) return // fast path

    val end = size - (this.buffer.size - this.endIndex)
    val buffer = ByteArray(size)
    this.buffer.copyInto(buffer, 0, startIndex = 0, endIndex = this.startIndex)
    this.buffer.copyInto(buffer, end, startIndex = this.endIndex, endIndex = this.buffer.size)
    this.endIndex = end
    this.buffer = buffer
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

  override val size: Int
    get() = this.buffer.size - capacity

  override val gap: Gap
    get() = this

  override fun get(offset: Int): Byte {
    // Preconditions.
    check(offset in 0 until this.size)

    // Convert the indices, without moving the gap.
    return this.buffer[offsetToIndex(offset)]
  }

  override fun set(offset: Int, value: Byte) {
    // Preconditions.
    check(offset in 0 until this.size)

    // Convert the indices, without moving the gap.
    this.buffer[offsetToIndex(offset)] = value
  }

  override fun push(byte: Byte, offset: Int) {
    // Preconditions.
    check(offset in 0..this.size)

    // Grow the buffer by one, then move to the insertion index, and finally insert the new element
    // into the gap by incrementing the startIndex.
    //
    // The ordering of growing then moving matters.
    grow(nextSize(capacity = 1))
    move(to = offset)
    this.buffer[this.startIndex++] = byte
  }

  override fun push(bytes: ByteArray, offset: Int, startIndex: Int, endIndex: Int) {
    // Range preconditions
    check(endIndex >= startIndex)
    check(startIndex in 0..bytes.size)
    check(endIndex in 0..bytes.size)
    check(offset in 0..this.size)

    // Optimizations.
    if (endIndex == startIndex) return

    // Grow the buffer by making sure we have enough capacity for the newly added elements. Then,
    // the cursor will be moved to the insertion index, and the input data will be copied starting
    // at the current cursor position. Finally, the startIndex is incremented.
    grow(nextSize(capacity = endIndex - startIndex))
    move(to = offset)
    bytes.copyInto(this.buffer, offset, startIndex, endIndex)
    this.startIndex += (endIndex - startIndex)
  }

  override fun copyInto(
      bytes: ByteArray,
      destinationOffset: Int,
      startOffset: Int,
      endOffset: Int
  ): ByteArray {
    // Preconditions.
    check(destinationOffset in 0..bytes.size)
    check(destinationOffset + endOffset - startOffset in 0..bytes.size)
    check(startOffset in 0..this.size)
    check(endOffset in 0..this.size)
    check(startOffset <= endOffset)

    // Optimizations.
    if (endOffset == startOffset) return bytes

    // We have to perform two chunk copies of the buffer, for the elements located before the gap,
    // and for the elements localed after the gap.
    return bytes.apply {
      buffer.copyInto(this, destinationOffset, startOffset, startIndex)
      buffer.copyInto(bytes, destinationOffset - startOffset + startIndex, endIndex, buffer.size)
    }
  }

  override fun remove(offset: Int, size: Int): ByteArray {
    // Preconditions.
    check(offset in 0 until this.size)
    check(offset + size in 0..this.size)
    move(to = offset)

    // Increment the endIndex, and then copy the previous size elements from the buffer into the
    // returned array. Because the cursor was moved, we can simply sequentially move them.
    this.endIndex += size
    return this.buffer.copyInto(
        destination = ByteArray(size),
        startIndex = this.endIndex - size,
        endIndex = this.endIndex,
    )
  }

  override fun clear() {
    // We can reset the buffer by having expanding the gap to the buffer boundaries.
    this.startIndex = 0
    this.endIndex = this.buffer.size
  }

  /**
   * Outputs a [String] representation of the [MutableByteGapBuffer]. Bytes which are part of the
   * gap will be prefixed by a '*' symbol.
   */
  override fun toString(): String {
    val content = bufferToString(buffer.toTypedArray(), startIndex until endIndex)
    return "ByteGapBuffer(start=$startIndex, end=$endIndex, data=[${content}])"
  }
}
