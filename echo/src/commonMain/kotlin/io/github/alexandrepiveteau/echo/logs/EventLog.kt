package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.EchoEventLogPreview
import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier

/**
 * An [EventLog] is a data structure that contains events of type [T], alongside with their unique
 * identifiers. An [EventLog] has no notion of "current site" or whatsoever; it only acts as the
 * backing store for a set of events, which can be retrieved and traversed in a more or less optimal
 * fashion.
 *
 * @param T the type of of the body of one event.
 */
interface EventLog<out T> {

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
   * Gets the event with a given [seqno] and [site], if it exists.
   *
   * @param seqno the sequence number of the event.
   * @param site the site of the event.
   */
  operator fun get(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Entry<T>?

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
  ): Iterable<Entry<T>>

  /**
   * An [Entry] in the event log, consisting of an event, and a unique identifier for the event.
   *
   * @param T the type of the events.
   */
  interface Entry<out T> {

    /** The unique identifier for this event. */
    val identifier: EventIdentifier

    /** The actual body of the event. */
    val body: T
  }
}
