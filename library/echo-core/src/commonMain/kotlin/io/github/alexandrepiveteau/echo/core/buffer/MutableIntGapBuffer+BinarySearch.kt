package io.github.alexandrepiveteau.echo.core.buffer

import io.github.alexandrepiveteau.echo.core.requireIn

// LINEAR SEARCH

/**
 * Returns the insertion index for the given [Int]. The insertion index can be seen as the index at
 * which the cursor should be moved when inserting an event with the given sequence number and site
 * identifier that would preserve the sorted behavior of the buffer.
 *
 * @param value the [Int] we would insert.
 *
 * @return the insertion index, between `0` and [MutableIntGapBuffer.size]
 *
 * @see MutableIntGapBuffer.binarySearch a binary search equivalent
 */
public fun MutableIntGapBuffer.linearSearch(
    value: Int,
): Int {
  var index = 0
  while (index < size) {
    if (get(index) == value) return index
    if (get(index) > value) return -index - 1 // Inverted insertion point.
    index++
  }
  return -size - 1
}

// BINARY SEARCH

/**
 * Searches this [MutableIntGapBuffer] for the provided [element] using the binary search algorithm.
 * The array is expected to be sorted, otherwise the result is undefined.
 *
 * If the list contains multiple elements equal to the specified element, there is no guarantee
 * which one will be found.
 *
 * The implementation is adapted from [List.binarySearch].
 *
 * @return the index of the element, if it is contained in the array within the specified range;
 * otherwise the inverted insertion point `(-insertion point - 1)`.
 */
public fun MutableIntGapBuffer.binarySearch(
    element: Int,
    fromIndex: Int = 0,
    toIndex: Int = size,
): Int {
  requireIn(fromIndex, 0, size + 1)
  requireIn(toIndex, 0, size + 1)

  var low = fromIndex
  var high = toIndex - 1
  while (low <= high) {
    val mid = (low + high).ushr(1) // safe from overflows
    val midVal = get(mid)
    val cmp = midVal.compareTo(element)
    when {
      cmp < 0 -> low = mid + 1
      cmp > 0 -> high = mid - 1
      else -> return mid
    }
  }
  return -(low + 1)
}
