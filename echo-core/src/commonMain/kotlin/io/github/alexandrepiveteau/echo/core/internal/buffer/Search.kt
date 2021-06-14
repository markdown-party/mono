package io.github.alexandrepiveteau.echo.core.internal.buffer

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier

/**
 * Uses binary search to find the insertion position of an event with the given [SequenceNumber] and
 * [SiteIdentifier].
 */
internal fun EventIdentifierGapBuffer.binarySearch(
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
