package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.MutableByteGapBuffer
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An interface defining an iterator over a [MutableEventLog]. Efficient, allocation-free methods
 * for iteration are available, as well as an implementation of [ListIterator] which returns [Event]
 * objects on each iteration.
 */
interface EventIterator : ListIterator<Event> {

  /**
   * Moves the [EventIterator] to the next event, without allocations.
   *
   * You should check that [hasNext] returns true before calling this method.
   */
  fun moveNext()

  /**
   * Moves the [EventIterator] to the previous event, without allocations.
   *
   * You should check that [hasPrevious] returns true before calling this method.
   */
  fun movePrevious()

  /** The [SequenceNumber] of the previous event. */
  val previousSeqno: SequenceNumber
  /** The [SiteIdentifier] of the previous event. */
  val previousSite: SiteIdentifier
  /** The [EventIdentifier] of the previous event. */
  val previousEventIdentifier: EventIdentifier
    get() = EventIdentifier(previousSeqno, previousSite)
  /** The body of the previous event, between [previousFrom] and [previousUntil]. */
  val previousEvent: MutableByteGapBuffer
  /** The start of the previous event body. */
  val previousFrom: Int
  /** The end (non-inclusive) of previous event body. */
  val previousUntil: Int

  /** The [SequenceNumber] of the next event. */
  val nextSeqno: SequenceNumber
  /** The [SiteIdentifier] of the next event. */
  val nextSite: SiteIdentifier
  /** The [EventIdentifier] of the next event. */
  val nextEventIdentifier: EventIdentifier
    get() = EventIdentifier(nextSeqno, nextSite)
  /** The body of the next event, between [nextFrom] and [nextUntil]. */
  val nextEvent: MutableByteGapBuffer
  /** The start of the next event body. */
  val nextFrom: Int
  /** The end (non-inclusive) of next event body. */
  val nextUntil: Int
}

/** Returns true iff the [EventIterator] is empty. */
fun EventIterator.isEmpty(): Boolean {
  return !hasPrevious() && !hasNext()
}

/** Returns true iff the [EventIterator] is not empty. */
fun EventIterator.isNotEmpty(): Boolean = !isEmpty()

/** Moves the [EventIterator] to its start, without allocations. */
fun EventIterator.moveToStart() {
  while (hasPrevious()) movePrevious()
}

/** Moves the [EventIterator] to its end, without allocations. */
fun EventIterator.moveToEnd() {
  while (hasNext()) moveNext()
}

/** Moves the [EventIterator] to the previous, until [predicate] is valid. */
inline fun EventIterator.movePreviousUntil(predicate: () -> Boolean) {
  while (hasPrevious() && !predicate()) movePrevious()
}

/**
 * Moves the [EventIterator] such that the next insertion position is available at
 * [MutableEventIterator.add].
 *
 * @param seqno the [SequenceNumber] for insertions.
 * @param site the [SiteIdentifier] for insertions.
 */
fun EventIterator.moveBefore(seqno: SequenceNumber, site: SiteIdentifier) = movePreviousUntil {
  previousEventIdentifier > EventIdentifier(seqno, site)
}

/**
 * A class representing an [Event]. This will typically be used when a function should return a
 * complete event, as well as its associated identifier.
 *
 * You should prefer using some allocation-free functions for high-performance code.
 *
 * @param seqno the [SequenceNumber] for this [Event].
 * @param site the [SiteIdentifier] for this [Event].
 * @param data the [ByteArray] that contains the [Event] body.
 */
@Serializable
@SerialName("evt")
data class Event(
    val seqno: SequenceNumber,
    val site: SiteIdentifier,
    val data: ByteArray,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || other !is Event) return false

    if (seqno != other.seqno) return false
    if (site != other.site) return false
    if (!data.contentEquals(other.data)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = seqno.hashCode()
    result = 31 * result + site.hashCode()
    result = 31 * result + data.contentHashCode()
    return result
  }
}
