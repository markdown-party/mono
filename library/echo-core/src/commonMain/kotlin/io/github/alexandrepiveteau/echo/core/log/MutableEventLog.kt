package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier

/**
 * A [MutableEventLog] is an implementation of [EventLog] optimized for consecutive insertions and
 * removals at the same index; this works particularly well when many events are appended to the end
 * of a [MutableEventLog], or when two [MutableEventLog] are merged.
 *
 * @see MutableHistory a variation of [MutableEventLog] which keeps a state
 */
public interface MutableEventLog : EventLog {

  /**
   * Inserts the provided event in the [MutableEventLog] at the appropriate index. If the event is
   * already present, or an event with the same [SiteIdentifier] has already been inserted, the
   * insertion will simply be ignored.
   *
   * This also means that if you have multiple events with the same [SiteIdentifier] at your
   * disposal, you should make sure to [insert] them in the increasing [SequenceNumber] order.
   *
   * @param seqno the [SequenceNumber] for the inserted event.
   * @param site the [SiteIdentifier] for the inserted event.
   * @param event the body of the event.
   * @param from where the event body should be read.
   * @param until where the event body should be read.
   */
  public fun insert(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: ByteArray,
      from: Int = 0,
      until: Int = event.size,
  )

  /**
   * Inserts the provided [Event] in the [MutableEventLog] at the appropriate index. If the event is
   * already present, or an event with the same [EventIdentifier] has already been inserted, the
   * insertion will simply be ignored.
   *
   * This also means that if you have multiple events with the same [SiteIdentifier] at your
   * disposal, you should make sure to [insert] them in the increasing [SequenceNumber] order.
   *
   * @param event the [Event] that is inserted.
   */
  public fun insert(
      event: Event,
  ): Unit = insert(seqno = event.seqno, site = event.site, event = event.data)

  /**
   * Appends a new event in the [MutableEventLog] for the given [SiteIdentifier]. The
   * [MutableEventLog] should be owned by the given [SiteIdentifier], to ensure no duplicate
   * insertions can occur.
   *
   * @param site the [SiteIdentifier] for the inserted event.
   * @param event the body of the event.
   * @param from where the event body should be read.
   * @param until where the event body should be read.
   */
  public fun append(
      site: SiteIdentifier,
      event: ByteArray,
      from: Int = 0,
      until: Int = event.size,
  ): EventIdentifier

  /**
   * Acknowledges the given [SequenceNumber] for a [SiteIdentifier]. The behavior of the
   * acknowledgement will be similar to what the [insert] method does.
   *
   * @param seqno the [SequenceNumber] for the acknowledged event.
   * @param site the [SiteIdentifier] for the acknowledged event.
   */
  public fun acknowledge(seqno: SequenceNumber, site: SiteIdentifier)

  /**
   * Acknowledges the latest [SequenceNumber] for each [SiteIdentifier] that the other
   * [MutableEventLog] contains. The behavior of the acknowledgement will be similar to what the
   * [acknowledge] method does.
   *
   * @param from the [MutableEventLog] from which acknowledgements are merged.
   * @return this [MutableEventLog] instance (with the new acknowledgements).
   */
  public fun acknowledge(from: EventLog): MutableEventLog

  /**
   * Merges this [MutableEventLog] [from] another log. The merge operation has the following
   * semantics :
   *
   * - Operations which occurred between this log and the [from] are merged into this log.
   * - This will [acknowledge] all the sites [from] the other [MutableEventLog].
   *
   * @param from the [EventLog] from which the operations are merged.
   * @return this [MutableEventLog] instance (with the new operations inserted).
   */
  public fun merge(from: EventLog): MutableEventLog

  /**
   * Removes the event with the given [seqno] and [site], assuming it's present in the
   * [MutableEventLog]. An event that is not present will not be removed, and will not be marked
   * acknowledged by the [MutableEventLog] either.
   *
   * @param seqno the [SequenceNumber] for the event to remove.
   * @param site the [SiteIdentifier] for the event to remove.
   *
   * @return a [Boolean] with value `true` iff the event was removed.
   */
  public fun remove(seqno: SequenceNumber, site: SiteIdentifier): Boolean

  /**
   * Clears the data of the [MutableEventLog]. This will not necessarily reset the [acknowledged]
   * events, which will still act as a baseline for event insertions and appends.
   *
   * Why use [clear] at all then, instead of creating a new [MutableEventLog] ?
   *
   * Because keeping the [acknowledged] meta-data is sometimes useful when performing
   * synchronization with multiple sites concurrently. We want to keep track of what we've already
   * seen and dispatched to a central [MutableEventLog] or [MutableHistory], but do not need to keep
   * track of the actual body of the events that were already sent.
   */
  public fun clear()
}
