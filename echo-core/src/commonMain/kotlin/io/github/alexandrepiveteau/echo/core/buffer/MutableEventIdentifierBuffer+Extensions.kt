package io.github.alexandrepiveteau.echo.core.buffer

import io.github.alexandrepiveteau.echo.core.causality.*

/** Creates an empty [MutableEventIdentifierGapBuffer]. */
fun mutableEventIdentifierGapBufferOf(): MutableEventIdentifierGapBuffer =
    MutableEventIdentifierGapBufferImpl()

/**
 * Creates a [MutableEventIdentifierGapBuffer] with a given [size], where each cell is init at zero.
 *
 * @throws IllegalArgumentException if the size is negative.
 */
fun MutableEventIdentifierGapBuffer(size: Int): MutableEventIdentifierGapBuffer {
  return MutableEventIdentifierGapBuffer(size) { EventIdentifier.Unspecified }
}

/**
 * Creates a [MutableEventIdentifierGapBuffer] with a given [size], where each item is [init].
 *
 * @throws IllegalArgumentException if the size is negative.
 */
fun MutableEventIdentifierGapBuffer(
    size: Int,
    init: (Int) -> EventIdentifier
): MutableEventIdentifierGapBuffer {
  require(size >= 0) { "MutableEventIdentifierGapBuffer requires a positive size." }
  return mutableEventIdentifierGapBufferOf().apply { repeat(size) { push(init(it)) } }
}

/**
 * Copies the contents of this [MutableEventIdentifierGapBuffer] into a new [EventIdentifierArray].
 */
fun MutableEventIdentifierGapBuffer.toEventIdentifierArray(): EventIdentifierArray {
  return copyInto(EventIdentifierArray(size))
}

/**
 * Copies the contents of this [MutableEventIdentifierGapBuffer] into a new [Array] of
 * [EventIdentifier].
 */
fun MutableEventIdentifierGapBuffer.toTypedArray(): Array<EventIdentifier> {
  return toEventIdentifierArray().toTypedArray()
}

/**
 * Uses binary search to find the insertion position of an event with the given [SequenceNumber] and
 * [SiteIdentifier].
 */
internal fun MutableEventIdentifierGapBuffer.binarySearch(
    seqno: SequenceNumber,
    site: SiteIdentifier,
): Int {
  // TODO : Use binary search rather than linear search from start.
  var position = 0
  while (true) {
    if (position == size) return size
    if (get(position) > EventIdentifier(seqno, site)) return position
    position++
  }
}
