package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.MutableByteGapBuffer
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier

/**
 * An interface defining an iterator over a [MutableEventLog]. Efficient, allocation-free methods
 * for iteration are available, as well as an implementation of [ListIterator] which returns [Event]
 * objects on each iteration.
 */
public interface EventIterator : ListIterator<Event> {

  /**
   * Moves the [EventIterator] to the next event, without allocations.
   *
   * You should check that [hasNext] returns true before calling this method.
   */
  public fun moveNext()

  /**
   * Moves the [EventIterator] to the previous event, without allocations.
   *
   * You should check that [hasPrevious] returns true before calling this method.
   */
  public fun movePrevious()

  /** The [SequenceNumber] of the previous event. */
  public val previousSeqno: SequenceNumber
  /** The [SiteIdentifier] of the previous event. */
  public val previousSite: SiteIdentifier
  /** The [EventIdentifier] of the previous event. */
  public val previousEventIdentifier: EventIdentifier
    get() = EventIdentifier(previousSeqno, previousSite)
  /** The body of the previous event, between [previousFrom] and [previousUntil]. */
  public val previousEvent: MutableByteGapBuffer
  /** The start of the previous event body. */
  public val previousFrom: Int
  /** The end (non-inclusive) of previous event body. */
  public val previousUntil: Int

  /** The [SequenceNumber] of the next event. */
  public val nextSeqno: SequenceNumber
  /** The [SiteIdentifier] of the next event. */
  public val nextSite: SiteIdentifier
  /** The [EventIdentifier] of the next event. */
  public val nextEventIdentifier: EventIdentifier
    get() = EventIdentifier(nextSeqno, nextSite)
  /** The body of the next event, between [nextFrom] and [nextUntil]. */
  public val nextEvent: MutableByteGapBuffer
  /** The start of the next event body. */
  public val nextFrom: Int
  /** The end (non-inclusive) of next event body. */
  public val nextUntil: Int
}

/** Returns true iff the [EventIterator] is empty. */
public fun EventIterator.isEmpty(): Boolean {
  return !hasPrevious() && !hasNext()
}

/** Returns true iff the [EventIterator] is not empty. */
public fun EventIterator.isNotEmpty(): Boolean = !isEmpty()

/** Moves the [EventIterator] to its start, without allocations. */
public fun EventIterator.moveToStart() {
  while (hasPrevious()) movePrevious()
}

/** Moves the [EventIterator] to its end, without allocations. */
public fun EventIterator.moveToEnd() {
  while (hasNext()) moveNext()
}

/** Moves the [EventIterator] to the previous, while the [predicate] is valid. */
public inline fun EventIterator.movePreviousWhile(predicate: EventIterator.() -> Boolean) {
  while (hasPrevious() && predicate()) movePrevious()
}
