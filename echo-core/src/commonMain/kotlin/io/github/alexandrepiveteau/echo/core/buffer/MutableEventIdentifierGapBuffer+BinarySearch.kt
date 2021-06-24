package io.github.alexandrepiveteau.echo.core.buffer

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.isSpecified

// LINEAR SEARCH

/**
 * Returns the insertion index for the given [EventIdentifier]. The insertion index can be seen as
 * the index at which the cursor should be moved when inserting an event with the given sequence
 * number and site identifier that would preserve the sorted behavior of the buffer.
 *
 * @param value the [EventIdentifier] we would insert.
 *
 * @return the insertion index, between `0` and [MutableEventIdentifierGapBuffer.size]
 *
 * @see MutableEventIdentifierGapBuffer.binarySearch a binary search equivalent
 */
fun MutableEventIdentifierGapBuffer.linearSearch(
    value: EventIdentifier,
): Int {
  require(value.isSpecified)
  var index = 0
  while (index < size) {
    if (get(index) > value) return index
    index++
  }
  return index
}

// BINARY SEARCH

/**
 * Returns the [EventIdentifier] that is before the given insertion index, or the minimum possible
 * [EventIdentifier] if at the first insertion index.
 */
private fun MutableEventIdentifierGapBuffer.before(
    index: Int,
): EventIdentifier {
  if (index == 0) return EventIdentifier(SequenceNumber.Unspecified, SiteIdentifier.Unspecified)
  return get(index - 1)
}

/**
 * Returns the [EventIdentifier] that is before the given insertion index, or the maximum possible
 * [EventIdentifier] if at the last insertion index.
 */
private fun MutableEventIdentifierGapBuffer.after(
    index: Int,
): EventIdentifier {
  if (index == size) return EventIdentifier(SequenceNumber.Max, SiteIdentifier.Max)
  return get(index)
}

/**
 * Returns true iff the given [id] would fit in an insertion range starting at [start] and ending at
 * [endInclusive].
 */
private fun MutableEventIdentifierGapBuffer.inRange(
    id: EventIdentifier,
    start: Int,
    endInclusive: Int,
): Boolean {
  val startId = before(start)
  val endInclusiveId = after(endInclusive)
  return startId <= id && id <= endInclusiveId
}

/**
 * A tail-recursive helper function, which finds the appropriate insertion position for the given
 * [EventIdentifier] within the range specified by [start] and [endInclusive].
 *
 * @return the insertion index.
 */
private tailrec fun MutableEventIdentifierGapBuffer.binarySearchHelper(
    id: EventIdentifier,
    start: Int,
    endInclusive: Int,
): Int {
  if (start == endInclusive) return start
  val mid = (start + endInclusive) / 2
  return if (inRange(id, start, mid)) binarySearchHelper(id, start, mid)
  else binarySearchHelper(id, mid + 1, endInclusive)
}

/**
 * Returns the insertion index for the given [EventIdentifier]. The insertion index can be seen as
 * the index at which the cursor should be moved when inserting an event with the given sequence
 * number and site identifier that would preserve the sorted behavior of the buffer.
 *
 * @param value the [EventIdentifier] we would insert.
 *
 * @return the insertion index, between `0` and [MutableEventIdentifierGapBuffer.size]
 *
 * @see MutableEventIdentifierGapBuffer.linearSearch a linear search equivalent
 */
fun MutableEventIdentifierGapBuffer.binarySearch(
    value: EventIdentifier,
): Int {
  require(value.isSpecified)
  return binarySearchHelper(value, 0, size)
}
