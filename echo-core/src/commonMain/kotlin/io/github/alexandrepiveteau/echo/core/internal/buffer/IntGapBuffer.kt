package io.github.alexandrepiveteau.echo.core.internal.buffer

import io.github.alexandrepiveteau.echo.core.causality.toTypedArray

/**
 * An [IntGapBuffer] is a high-performance mutable list of integers, which are concatenated one
 * after each other in a contiguous [IntArray]. An [IntGapBuffer] is optimized for consecutive
 * insertions and removals at the same index.
 */
internal class IntGapBuffer {

  /** The index at which new events should be inserted in the [events] array. */
  private var gapStart: Int = 0

  /** The index at which the insertion gap ends, non-inclusive. */
  private var gapEnd: Int = DefaultGapBufferSize

  /** A contiguous sequence of ints, representing the events in the log. */
  private var events = IntArray(DefaultGapBufferSize)

  // LOW-LEVEL GAP BUFFER

  /** How many [Int] can be written without having to re-grow the backing array. */
  private val capacity: Int
    get() = gapEnd - gapStart

  /** Moves the gap by an amount of one to the left, or no-ops if already at a boundary. */
  private fun left() {
    if (gapStart > 0) {
      gapStart--
      gapEnd--
      events[gapEnd] = events[gapStart]
    }
  }

  /** Moves the gap by an amount of one to the right, or no-ops if already at a boundary. */
  private fun right() {
    if (gapEnd < events.size) {
      events[gapStart] = events[gapEnd]
      gapStart++
      gapEnd++
    }
  }

  /**
   * Grows the gap buffer until at least [capacity] may be fulfilled by the buffer. This takes an
   * amortized constant time in the resulting size.
   */
  private fun grow(capacity: Int) {
    while (this.capacity < capacity) {
      // Evaluate new size.
      var newSize = events.size * 2
      if (newSize == 0) newSize = DefaultGapBufferSize
      if (newSize < 0) newSize = Int.MAX_VALUE // Avoid overflows.

      // Adjust pointers.
      val newStart = gapStart
      val newEnd = newSize - (events.size - gapEnd)

      // Create and batch copy data in the buffer.
      val buffer = IntArray(newSize)
      events.copyInto(buffer, 0, startIndex = 0, endIndex = gapStart)
      events.copyInto(buffer, newEnd, startIndex = gapEnd, endIndex = events.size)

      // Set values.
      gapStart = newStart
      gapEnd = newEnd
      events = buffer
    }
  }

  /** Returns the [Int] at the given [index] in the [events] buffer. */
  operator fun get(index: Int): Int {
    if (index < 0 || index >= events.size - capacity) throw IndexOutOfBoundsException()
    return if (index < gapStart) {
      events[index]
    } else {
      events[index + capacity]
    }
  }

  /** Sets the value at the provided index in the [events] buffer. */
  operator fun set(index: Int, value: Int) {
    if (index < 0 || index >= events.size - capacity) throw IndexOutOfBoundsException()
    return if (index < gapStart) {
      events[index] = value
    } else {
      events[index + capacity] = value
    }
  }

  /** Returns an [IntArray] that contains a copy of the events from this buffer. */
  fun toArray(): IntArray {
    return IntArray(size).apply {
      events.copyInto(this, 0, startIndex = 0, endIndex = gapStart)
      events.copyInto(this, gapStart, startIndex = gapEnd, endIndex = events.size)
    }
  }

  /** Moves the cursor until the provided [index] is reached. */
  private tailrec fun move(index: Int): Unit =
      when {
        gapStart > index -> {
          left()
          move(index)
        }
        gapStart < index -> {
          right()
          move(index)
        }
        else -> Unit
      }

  /** Clears this [IntGapBuffer] and removes all its content. */
  fun clear() {
    gapStart = 0
    gapEnd = events.size
  }

  // DELICATE BUFFER MANAGEMENT

  /**
   * The "cursor" index, represented as the start of the gap. Inserting before the first int results
   * in a cursor value of 0, and inserting at the end of the sequence results in a cursor value of
   * [size]
   */
  val cursor: Int
    @DelicateGapBufferApi get() = gapStart

  /**
   * Moves the [cursor] by a certain amount, respecting size conditions. Improper movements may lead
   * to degraded performance.
   */
  @DelicateGapBufferApi
  fun shift(amount: Int) {
    move(index = cursor + amount)
  }

  // HIGH-LEVEL GAP BUFFER MANAGEMENT

  /** Returns the [size] of this gap buffer, aka. how many items it contains. */
  val size: Int
    get() = events.size - capacity

  /** Pushes a single [Int] onto the [events] buffer, at the provided index. */
  fun push(int: Int, index: Int = size) {
    if (index < 0 || index > size) throw IndexOutOfBoundsException()
    grow(capacity = 1)
    move(index)
    events[gapStart++] = int
  }

  /** Pushes a [IntArray] onto the [events] buffer, at the provided index. */
  fun push(ints: IntArray, index: Int = size) {
    if (index < 0 || index > size) throw IndexOutOfBoundsException()
    grow(capacity = ints.size)
    move(index)
    for (int in ints) {
      events[gapStart++] = int
    }
  }

  /** Removes the element at the given [index] from the gap buffer. */
  fun remove(index: Int): Int {
    if (index < 0 || index >= size) throw IndexOutOfBoundsException()
    move(index)
    return events[gapEnd++]
  }

  /**
   * Outputs a [String] representation of the [IntGapBuffer]. Ints which are part of the gap will be
   * prefixed by a '*' symbol.
   */
  override fun toString(): String {
    val content = bufferToString(events.toTypedArray(), gapStart until gapEnd)
    return "IntGapBuffer(start=$gapStart, end=$gapEnd, data=[${content}])"
  }
}
