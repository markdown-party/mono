package io.github.alexandrepiveteau.echo.core.causality

import io.github.alexandrepiveteau.echo.core.requireIn

/**
 * Searches this [EventIdentifierArray] for the provided [element] using the binary search
 * algorithm. The array is expected to be sorted, otherwise the result is undefined.
 *
 * If the list contains multiple elements equal to the specified element, there is no guarantee
 * which one will be found.
 *
 * The implementation is adapted from [List.binarySearch].
 *
 * @return the index of the element, if it is contained in the array within the specified range;
 * otherwise the inverted insertion point `(-insertion point - 1)`.
 */
public fun EventIdentifierArray.binarySearch(
    element: EventIdentifier,
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

/**
 * Searches this [EventIdentifierArray] for the provided [element] using the binary search
 * algorithm. The array is expected to be sorted by site identifiers, otherwise the result is
 * undefined.
 *
 * If the list contains multiple elements with a site equal to the specified element, there is no
 * guarantee which one will be found.
 *
 * The implementation is adapted from [List.binarySearch].
 *
 * @return the index of the element with the given site identifier, if it is contained in the array
 * within the specified range; otherwise the inverted insertion point `(-insertion point - 1)`.
 */
public fun EventIdentifierArray.binarySearchBySite(
    element: SiteIdentifier,
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
    val cmp = midVal.site.compareTo(element)
    when {
      cmp < 0 -> low = mid + 1
      cmp > 0 -> high = mid - 1
      else -> return mid
    }
  }
  return -(low + 1)
}
