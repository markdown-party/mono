package party.markdown.rga

import io.github.alexandrepiveteau.echo.core.buffer.*
import io.github.alexandrepiveteau.echo.core.causality.*

/**
 * A [MutableRGA] aggregates [RGAEvent] and mutates its internal state to compute a distributed
 * sequence of [Char].
 *
 * Conceptually, a [MutableRGA] stores all the characters that were inserted in the distributed
 * sequence. When a character is inserted, it references an anchor. The nodes are then traversed
 * using BFS, sorting adjacent nodes by their insertion timestamp.
 *
 * Additionally, characters may get deleted. In this case, their content is discarded, but their
 * identifier may be kept in memory to guarantee the ordering of future insertions.
 */
class MutableRGA {

  private companion object {

    /**
     * The [Char] that indicates that a certain atom has been removed, and should not be included in
     * the [toCharArray] call.
     */
    const val REMOVED = '\u0000'

    /** A value that indicates that an index was not (yet) found. */
    const val NOT_FOUND = -1
  }

  /**
   * A buffer with pending insertions, that is inserts for which the offset dependency is not
   * satisfied yet. Whenever a new event is effectively inserted, this buffer will be checked for
   * pending insertions which have to be applied.
   */
  private val pendingInserts = MutablePendingInsertBuffer()

  /**
   * A buffer with event identifiers that were deleted but were not integrated yet in the
   * [MutableRGA]. This ensures convergence even if removals are integrated before the atoms are
   * added.
   */
  private val pendingRemovals = MutableEventIdentifierGapBuffer(0)

  private val chars = MutableCharGapBuffer(0)
  private val identifiers = MutableEventIdentifierGapBuffer(0)
  private val offsets = MutableEventIdentifierGapBuffer(0)

  private fun offsetInRGA(offset: EventIdentifier): Boolean {
    var offsetInRGA = offset.isUnspecified
    for (i in 0 until identifiers.size) {
      if (identifiers[i] == offset) offsetInRGA = true
    }
    return offsetInRGA
  }

  /**
   * Inserts a [Char] atom after the given [EventIdentifier] offset. The inserted [Char] has a
   * unique identifier.
   *
   * @param site the [SiteIdentifier] for the identifier of the inserted atom.
   * @param seqno the [SequenceNumber] for the identifier of the inserted atom.
   * @param value the value of the inserted atom.
   * @param offset the [EventIdentifier] after which we're interested in inserting.
   */
  fun insert(
      site: SiteIdentifier,
      seqno: SequenceNumber,
      value: Char,
      offset: EventIdentifier,
  ) {
    if (offsetInRGA(offset)) {
      insertOne(site, seqno, value, offset)
      insertPending(EventIdentifier(seqno, site))
    } else {
      pendingInserts.add(site, seqno, value, offset)
    }
  }

  // A (reused) buffer that indicates the identifiers of the events that should be inserted from
  // the pending pool.
  private val pendingIdentifiers = ArrayDeque<EventIdentifier>()

  private fun insertPending(offset: EventIdentifier) {
    pendingIdentifiers.addLast(offset)
    while (pendingIdentifiers.isNotEmpty()) {
      val currentOffset = pendingIdentifiers.first()
      val next = pendingInserts.next(currentOffset)
      if (next < 0) {
        // We've exhausted all the events waiting for that offset.
        pendingIdentifiers.removeFirst()
        continue
      }
      insertOne(
          pendingInserts.identifier(next).site,
          pendingInserts.identifier(next).seqno,
          pendingInserts.value(next),
          pendingInserts.offset(next),
      )
      pendingIdentifiers.addLast(pendingInserts.identifier(next))
      pendingInserts.remove(next)
    }
  }

  // Actually insert the given char at the provided index.
  private fun insertOne(
      site: SiteIdentifier,
      seqno: SequenceNumber,
      value: Char,
      offset: EventIdentifier,
  ) {
    // Inspired by the Optimized RGA merge algorithm. There are some changes though:
    //
    // 1. If an anchor is not (yet) known, the insertion is postponed and this method not called !
    // 2. If the node was deleted before being inserted, the deletion is applied properly.
    // 3. No allocations are required to store the nodes.
    //
    // https://github.com/concordant/c-crdtlib/blob/94d0c3bf666ec0f3f30a3bfda8b295c619673b01/src/commonMain/kotlin/crdtlib/crdt/RGA.kt
    var anchor = NOT_FOUND
    var insertion = NOT_FOUND
    var i = 0

    while (i < chars.size) {
      when {
        // 1. Do not insert node duplicates.
        identifiers[i] == EventIdentifier(seqno, site) -> return

        // 2. Mark the anchor, aka. the offset that we're referencing. Additionally, we'll be
        // marking  that we're interested in inserting the character after the found offset.
        identifiers[i] == offset && anchor == NOT_FOUND -> {
          anchor = i
          insertion = NOT_FOUND
        }

        // 3. We've found the index at which we're interested in inserting the node. The most recent
        // insertions have to come directly after the current node, to guarantee good RGA semantics.
        EventIdentifier(seqno, site) > identifiers[i] && insertion == NOT_FOUND -> {
          insertion = i
          if (anchor > -1) break
        }
      }

      // Increment the index.
      i++
    }

    // Mark the value as removed if relevant, and remove it from the pending removals.
    val removedIndex = pendingRemovals.binarySearch(EventIdentifier(seqno, site))
    val insertedValue = if (removedIndex >= 0) REMOVED else value
    if (removedIndex >= 0) pendingRemovals.remove(removedIndex)

    // Finally, insert at the insertion point (or at the end of the buffer if more no insertion
    // point was found.
    when {
      insertion != NOT_FOUND -> {
        identifiers.push(EventIdentifier(seqno, site), offset = insertion)
        chars.push(insertedValue, offset = insertion)
        offsets.push(offset, offset = insertion)
      }
      else -> {
        identifiers.push(EventIdentifier(seqno, site))
        chars.push(insertedValue)
        offsets.push(offset)
      }
    }
  }

  /**
   * Removes the given [EventIdentifier] from the [MutableRGA]. If the atom at the given [offset] is
   * not integrated yet, the [offset] will be temporarily buffered and the removal applied when the
   * relevant [insert] operation occurs.
   */
  fun remove(offset: EventIdentifier) {
    for (i in 0 until identifiers.size) {
      if (identifiers[i] == offset) {
        chars[i] = REMOVED
        return
      }
    }

    // Add the offset to the pending removals.
    val index = pendingRemovals.binarySearch(offset)
    if (index >= 0) return // Already pending for removal
    pendingRemovals.push(offset, offset = -(index + 1))
  }

  /**
   * Returns a new [EventIdentifierArray] with all the identifiers from the [MutableRGA],
   * concatenated. The characters which were deleted will not see their [EventIdentifier] be
   * included in the [EventIdentifierArray].
   */
  fun toEventIdentifierArray(): EventIdentifierArray {
    val buffer = MutableEventIdentifierGapBuffer(0)
    for (i in 0 until identifiers.size) {
      if (chars[i] != REMOVED) {
        buffer.push(identifiers[i])
      }
    }
    return buffer.toEventIdentifierArray()
  }

  /**
   * Returns a new [CharArray] built with all the characters from the [MutableRGA], concatenated.
   * The characters which were deleted will not be included in the [CharArray].
   */
  fun toCharArray(): CharArray {
    val buffer = MutableCharGapBuffer(0)
    for (i in 0 until chars.size) {
      if (chars[i] != REMOVED) {
        buffer.push(chars[i])
      }
    }
    return buffer.toCharArray()
  }
}
