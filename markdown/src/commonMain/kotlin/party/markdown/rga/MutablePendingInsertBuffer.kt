package party.markdown.rga

import io.github.alexandrepiveteau.echo.core.buffer.MutableCharGapBuffer
import io.github.alexandrepiveteau.echo.core.buffer.MutableEventIdentifierGapBuffer
import io.github.alexandrepiveteau.echo.core.buffer.binarySearch
import io.github.alexandrepiveteau.echo.core.buffer.linearSearch
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier

/**
 * A buffer of pending insertion events. The pending events will be sorted by [EventIdentifier] of
 * the offset, making retrieval efficient using binary search when the offset dependency is
 * fulfilled.
 */
internal class MutablePendingInsertBuffer {

  private val offsets = MutableEventIdentifierGapBuffer(0)
  private val ids = MutableEventIdentifierGapBuffer(0)
  private val values = MutableCharGapBuffer(0)

  private fun insertionIndex(offset: EventIdentifier): Int {
    val index = offsets.binarySearch(offset)
    return if (index >= 0) index else -(index + 1) // inverted insertion point
  }

  /**
   * Adds a new pending event to this [MutablePendingInsertBuffer]. Duplicate events may be
   * inserted, so you should make sure not to integrate them twice in the RGA.
   *
   * @param site the [SiteIdentifier] for the event.
   * @param seqno the [SequenceNumber] for the event.
   * @param value the [Char] for the event.
   * @param offset the dependency for the event.
   */
  fun add(
      site: SiteIdentifier,
      seqno: SequenceNumber,
      value: Char,
      offset: EventIdentifier,
  ) {
    val index = insertionIndex(offset)
    offsets.push(offset, offset = index)
    ids.push(EventIdentifier(seqno, site), offset = index)
    values.push(value, offset = index)
  }

  /**
   * Returns the index of the next value that matches the given [offset]. If no value corresponds, a
   * negative value will be returned instead.
   */
  fun next(offset: EventIdentifier): Int {
    return offsets.binarySearch(offset)
  }

  fun index(id: EventIdentifier): Int {
    return ids.linearSearch(id)
  }

  /** Removes the value at the given [index]. */
  fun remove(index: Int) {
    offsets.remove(index)
    ids.remove(index)
    values.remove(index)
  }

  /** Returns the [EventIdentifier] that's present at the given [index]. */
  fun identifier(index: Int): EventIdentifier = ids[index]

  /** Returns the offset that's present at the given [index]. */
  fun offset(index: Int): EventIdentifier = offsets[index]

  /** Returns the [Char] that's present at the given [index]. */
  fun value(index: Int): Char = values[index]
}
