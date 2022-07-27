package io.github.alexandrepiveteau.echo.core.buffer

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import io.github.alexandrepiveteau.echo.core.causality.toTypedArray

/** Creates an empty [MutableEventIdentifierGapBuffer]. */
public fun mutableEventIdentifierGapBufferOf(): MutableEventIdentifierGapBuffer =
    MutableEventIdentifierGapBufferImpl()

/**
 * Creates a [MutableEventIdentifierGapBuffer] with a given [size], where each cell is init at zero.
 *
 * @throws IllegalArgumentException if the size is negative.
 */
public fun MutableEventIdentifierGapBuffer(size: Int): MutableEventIdentifierGapBuffer {
  return MutableEventIdentifierGapBuffer(size) { EventIdentifier.Unspecified }
}

/**
 * Creates a [MutableEventIdentifierGapBuffer] with a given [size], where each item is [init].
 *
 * @throws IllegalArgumentException if the size is negative.
 */
public fun MutableEventIdentifierGapBuffer(
    size: Int,
    init: (Int) -> EventIdentifier
): MutableEventIdentifierGapBuffer {
  require(size >= 0) { "MutableEventIdentifierGapBuffer requires a positive size." }
  return mutableEventIdentifierGapBufferOf().apply { repeat(size) { push(init(it)) } }
}

/**
 * Copies the contents of this [MutableEventIdentifierGapBuffer] into a new [EventIdentifierArray].
 */
public fun MutableEventIdentifierGapBuffer.toEventIdentifierArray(): EventIdentifierArray {
  return copyInto(EventIdentifierArray(size))
}

/**
 * Copies the contents of this [MutableEventIdentifierGapBuffer] into a new [Array] of
 * [EventIdentifier].
 */
public fun MutableEventIdentifierGapBuffer.toTypedArray(): Array<EventIdentifier> {
  return toEventIdentifierArray().toTypedArray()
}

/**
 * Copies the contents of this [EventIdentifierArray] into a new [MutableEventIdentifierGapBuffer].
 */
public fun EventIdentifierArray.toMutableGapBuffer(): MutableEventIdentifierGapBuffer {
  return MutableEventIdentifierGapBuffer(size, this::get)
}

/** Copies the given range from the [MutableEventIdentifierGapBuffer]. */
public fun MutableEventIdentifierGapBuffer.copyOfRange(
    from: Int,
    until: Int,
): EventIdentifierArray {
  val size = until - from
  require(size >= 0) { "Can't copy a negative range." }
  return copyInto(EventIdentifierArray(size), startOffset = from, endOffset = until)
}
