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
interface MutableEventLog : EventLog {

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
  fun insert(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: ByteArray,
      from: Int = 0,
      until: Int = event.size,
  )

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
  fun append(
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
  fun acknowledge(seqno: SequenceNumber, site: SiteIdentifier)

  /**
   * Acknowledges the latest [SequenceNumber] for each [SiteIdentifier] that the other
   * [MutableEventLog] contains. The behavior of the acknowledgement will be similar to what the
   * [acknowledge] method does.
   *
   * @param from the [MutableEventLog] from which acknowledgements are merged.
   * @return this [MutableEventLog] instance (with the new acknowledgements).
   */
  // TODO : Accept an EventLog.
  fun acknowledge(from: MutableEventLog): MutableEventLog

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
  fun merge(from: EventLog): MutableEventLog

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
  fun clear()
}
