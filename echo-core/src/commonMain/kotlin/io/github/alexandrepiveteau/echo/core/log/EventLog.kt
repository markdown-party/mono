package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier

/**
 * An [EventLog] is a high-performance mutable log of serialized events, which are concatenated
 * after each other in some contiguous data structures.
 *
 * @see MutableEventLog a mutable extension of this collection.
 */
interface EventLog {

  /** Returns the count of events in the [EventLog]. */
  val size: Int

  /**
   * Returns true if the event with the event with the given [SiteIdentifier] is included in the
   * [EventLog], or if any event with the same [SiteIdentifier] and a higher [SequenceNumber] has
   * already been integrated.
   *
   * @param seqno the [SequenceNumber] to check for.
   * @param site the [SiteIdentifier] to check for.
   *
   * @return `true` iff the event was already integrated.
   */
  fun contains(seqno: SequenceNumber, site: SiteIdentifier): Boolean

  /**
   * Returns an [EventIdentifierArray] with all the acknowledgements that have been issued by this
   * [MutableEventLog]. This [EventIdentifierArray] only contains one [EventIdentifier] per
   * [SiteIdentifier].
   *
   * The returned [EventIdentifierArray] will be sorted by [SiteIdentifier].
   */
  fun acknowledged(): EventIdentifierArray

  /**
   * Returns an [EventIterator]. The retrieved [EventIterator] will start at the end of the
   * [MutableEventLog], and should not be used anymore if the underlying [MutableEventLog] is
   * modified.
   */
  operator fun iterator(): EventIterator

  /**
   * Returns an [EventIterator] specific to a single site. The retrieved [EventIdentifier] will
   * start at the end of the [MutableEventLog], and should not be used anymore if the underlying
   * [EventLog] is modified.
   *
   * @param site the [SiteIdentifier] for which the [EventIterator] is retrieved.
   */
  fun iterator(site: SiteIdentifier): EventIterator
}
