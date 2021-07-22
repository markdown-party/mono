package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.MutableByteGapBuffer
import io.github.alexandrepiveteau.echo.core.buffer.copyOfRange
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

  override fun next(): Event {
    check(hasNext()) { "No next element." }
    moveNext()
    return Event(seqno, site, event.copyOfRange(from, until))
  }

  override fun previous(): Event {
    check(hasPrevious()) { "No previous element." }
    movePrevious()
    return Event(seqno, site, event.copyOfRange(from, until))
  }

  /** Returns true if the iterator has an item at the current index. */
  fun has(): Boolean

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

  /** The [SequenceNumber] of the current event. */
  val seqno: SequenceNumber

  /** The [SiteIdentifier] of the current event. */
  val site: SiteIdentifier

  /** The body of the events. You should only read the contents in the [from] to [until] range. */
  val event: MutableByteGapBuffer

  /** The start of the event body. */
  val from: Int

  /** The end (non-inclusive) of the event body. */
  val until: Int
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
