package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.EchoEventLogPreview
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier

/**
 * An [ImmutableEventLog] is a data structure that contains events of type [T], alongside with their
 * unique identifiers. An [ImmutableEventLog] has no notion of "current site" or whatsoever; it only
 * acts as the backing store for a set of events, which can be retrieved and traversed in a more or
 * less optimal fashion.
 *
 * This collection is immutable, meaning that it can't mutated.
 *
 * @param T the type of of the body of one event.
 */
interface ImmutableEventLog<out T> {

  /** Returns an [Iterable] of all the [SiteIdentifier] that are known to this [EventLog]. */
  val sites: Set<SiteIdentifier>

  /**
   * Returns the [SequenceNumber] that is expected for the next event, such that this
   * [SequenceNumber] is higher than any [SequenceNumber] from the [EventLog].
   */
  @EchoEventLogPreview val expected: SequenceNumber

  /** Returns the [SequenceNumber] that is expected from this [EventLog]. */
  fun expected(site: SiteIdentifier): SequenceNumber

  /**
   * Gets the body of the event with a given [seqno] and [site], if it exists.
   *
   * @param seqno the sequence number of the event.
   * @param site the site of the event.
   */
  operator fun get(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): T?

  /**
   * Returns all the events greater or equal to the provided [SequenceNumber] for the given
   * [SiteIdentifier].
   *
   * @param seqno the lowest sequence number of the expected events.
   * @param site the site of the event.
   *
   * @return all the events that are equal or higher to this [seqno] for the [site].
   */
  fun events(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Iterable<EventValue<T>>

  /** Transforms this [ImmutableEventLog] to a persistable instance. */
  fun toPersistentEventLog(): PersistentEventLog<T>
}
