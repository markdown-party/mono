package io.github.alexandrepiveteau.echo.core

import io.github.alexandrepiveteau.echo.core.buffer.*
import io.github.alexandrepiveteau.echo.core.causality.*

/**
 * An [EventLog] is a high-performance mutable list of serialized events, which are concatenated one
 * after each other in a contiguous [ByteArray]. An [EventLog] is optimized for consecutive
 * insertions and removals at the same index; this works particularly well when many events are
 * appended to the end of the [EventLog].
 */
class EventLog {

  // INTERNAL STATE

  /** The [MutableByteGapBuffer] in which the events are individually managed. */
  private val events = mutableByteGapBufferOf()

  /** The [EventIdentifierGapBuffer] in which the event identifiers are individually managed. */
  // TODO : Use an optimized (delta + run-length encoding) identifiers buffer.
  private val identifiers = mutableEventIdentifierGapBufferOf()

  /**
   * The [MutableIntGapBuffer] in which the event sites are individually managed. This is used in
   * conjunction with the [events] content buffer.
   */
  private val sizes = mutableIntGapBufferOf()

  // TODO : Use a tree-like (ev. heap-like ?) data structure instead.
  /** An array of all the acknowledged event identifiers. */
  private val acknowledged = mutableEventIdentifierGapBufferOf()

  // INTERNAL LOW-LEVEL OPERATIONS

  /** Moves the internal event cursor by one event to the left. */
  private fun left() {
    if (sizes.gap.startIndex > 0) {
      // If we have at least one event to the left, shift the events buffer by the size of the
      // event, and the identifiers and sizes by minus one.
      val size = sizes[sizes.gap.startIndex - 1]
      events.gap.shift(-size)
      identifiers.gap.shift(-1)
      sizes.gap.shift(-1)
    }
  }

  /** Moves the internal event cursor by one event to the right. */
  private fun right() {
    if (sizes.gap.startIndex < sizes.size) {
      // If we are not at the end of the buffer, shift the events buffer by the size of the event,
      // and the identifiers and sizes by one.
      val size = sizes[sizes.gap.startIndex]
      events.gap.shift(size)
      identifiers.gap.shift(1)
      sizes.gap.shift(1)
    }
  }

  /** Moves the cursor by the provided [amount], using the [left] and [right] methods. */
  private fun move(amount: Int): Unit =
      when {
        amount > 0 -> {
          right()
          move(amount - 1)
        }
        amount < 0 -> {
          left()
          move(amount + 1)
        }
        else -> Unit
      }

  // PUBLIC API -- TODO : Provide an interface so compaction is possible for specific use-cases ?

  /** Returns the count of operations in the [EventLog]. */
  val size: Int
    get() = identifiers.size

  /**
   * Returns true if the event with the given [SiteIdentifier] and [SequenceNumber] is included in
   * the [EventLog], or if any event with the same [SiteIdentifier] and a higher [SequenceNumber]
   * has already been integrated.
   *
   * @param site the [SiteIdentifier] to check for.
   * @param seqno the [SequenceNumber] to check for.
   */
  fun contains(
      site: SiteIdentifier,
      seqno: SequenceNumber,
  ): Boolean {
    check(site.isSpecified) { "Site must be specified." }
    check(seqno.isSpecified) { "Sequence number must be specified." }
    for (i in 0 until acknowledged.size) {
      if (acknowledged[i].site == site) return acknowledged[i].seqno >= seqno
    }
    return false
  }

  /**
   * Inserts the provided event in the [EventLog] at the appropriate index. If the event is already
   * present, or an event with the same [SiteIdentifier] has already been inserted, the insertion
   * will simply be ignored.
   *
   * @param event the body of the event.
   * @param site the [SiteIdentifier] for the inserted event.
   * @param seqno the [SequenceNumber] for the inserted event.
   */
  // TODO : How can we manage changes ?
  fun insert(
      event: ByteArray,
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ) {
    check(seqno.isSpecified) { "Sequence number must be specified." }
    check(site.isSpecified) { "Site must be specified." }

    // Skip already inserted operations.
    if (contains(site, seqno)) return

    // Move either right or left, until the right position is reached.
    // TODO : Fast placement for consecutive insertions, rather than full-fledged binary search ?
    val position = identifiers.binarySearch(EventIdentifier(seqno, site))
    while (position > sizes.gap.startIndex) right()
    while (position < sizes.gap.startIndex) left()

    // Find the insertion index, and add the operation.
    events.push(event, offset = events.gap.startIndex)
    identifiers.push(EventIdentifier(seqno, site), offset = identifiers.gap.startIndex)
    sizes.push(event.size, offset = sizes.gap.startIndex)
    acknowledge(seqno, site)
  }

  /** Returns the maximum [SequenceNumber] that has been attributed to any site in the log. */
  private fun maxSeqno(): SequenceNumber {
    var max = SequenceNumber.Unspecified
    for (i in 0 until acknowledged.size) {
      max = maxOf(max, acknowledged[i].seqno)
    }
    return max
  }

  /** Updates the [acknowledged] buffer with a new maximum for a given site. */
  // TODO : Update with a tree-like data structure.
  private fun acknowledge(seqno: SequenceNumber, site: SiteIdentifier) {
    // TODO : What should we do on overflows ?
    for (i in 0 until acknowledged.size) {
      val identifier = acknowledged[i]
      if (identifier.site == site) {
        acknowledged[i] = EventIdentifier(maxOf(seqno, identifier.seqno), site)
        return // fast return
      }
    }
    // We didn't find the expected site, so we'll add it at the end of the gap buffer.
    acknowledged.push(EventIdentifier(seqno, site))
  }

  /**
   * Appends the provided event in the [EventLog] at the end, and returns the [EventIdentifier] that
   * has been given to the event. In order to [append], the caller should make sure it owns the
   * [EventLog] and that no two events with the same [SiteIdentifier] will be generated
   * concurrently.
   *
   * @param event the body of the event.
   * @param site the [SiteIdentifier] for the appended event.
   */
  fun append(
      event: ByteArray,
      site: SiteIdentifier,
  ): EventIdentifier {
    check(site.isSpecified) { "Site must be specified." }

    val seqno = maxSeqno().inc()
    return EventIdentifier(seqno, site).apply {
      events.push(event)
      identifiers.push(this)
      sizes.push(event.size)
      acknowledge(seqno, site)
    }
  }

  /**
   * Returns an [EventIdentifierArray] with all the known sites, and the maximum sequence number for
   * each site.
   */
  fun acknowledged(): EventIdentifierArray {
    return acknowledged.toEventIdentifierArray()
  }

  /**
   * Merges the provided [EventLog] in the current [EventLog]. This method takes a linear time in
   * the amount of shared operations.
   */
  fun merge(from: EventLog) {
    TODO("Not supported yet $from")
  }

  /** Clears this [EventLog], removing all the contained operations. This method takes O(1). */
  fun clear() {
    events.clear()
    identifiers.clear()
    sizes.clear()
    acknowledged.clear()
  }
}
