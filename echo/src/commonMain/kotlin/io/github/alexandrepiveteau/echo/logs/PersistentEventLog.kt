package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier

/**
 * A [PersistentEventLog] is an [ImmutableEventLog] which supports the insertion of events. It is an
 * immutable collection.
 */
interface PersistentEventLog<out T> : ImmutableEventLog<T> {

  /**
   * Sets the body of the event with a given [seqno] and [site].
   *
   * @param site the site of the event.
   * @param seqno the sequence number of the event.
   * @param body the body of the event.
   */
  fun set(
      site: SiteIdentifier,
      seqno: SequenceNumber,
      body: @UnsafeVariance T,
  ): PersistentEventLog<T>

  /**
   * Removes the event with a given [seqno] and [site]. If the event is not present, the data
   * structure remains unmodified.
   *
   * @param site the site of the event.
   * @param seqno the sequence number of the event.
   */
  fun remove(
      site: SiteIdentifier,
      seqno: SequenceNumber,
  ): PersistentEventLog<T>
}
