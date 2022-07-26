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
   * Returns true if the event with the event with the given [EventIdentifier] is included in the
   * [EventLog], or if any event with the same [SiteIdentifier] and a higher [SequenceNumber] has
   * already been integrated.
   *
   * @param id the [EventIdentifier] to check for.
   *
   * @return `true` iff the event was already integrated.
   */
  operator fun contains(id: EventIdentifier) = contains(id.seqno, id.site)

  /**
   * Returns an [EventIdentifierArray] with all the acknowledgements that have been issued by this
   * [MutableEventLog]. This [EventIdentifierArray] only contains one [EventIdentifier] per
   * [SiteIdentifier].
   *
   * The returned [EventIdentifierArray] will be sorted by [SiteIdentifier].
   */
  fun acknowledged(): EventIdentifierArray

  /**
   * Returns an [EventIterator]. The retrieved [EventIterator] will start at the beginning of the
   * [EventLog], and should not be used anymore if the underlying [EventLog] is modified.
   */
  operator fun iterator(): EventIterator

  /**
   * Returns an [EventIterator]. The retrieved [EventIterator] will start at the end of the
   * [EventLog], and should not be used anymore if the underlying [EventLog] is modified.
   */
  fun iteratorAtEnd(): EventIterator

  /**
   * Returns an [EventIterator] specific to a single site. The retrieved [EventIterator] will start
   * at the beginning of the [EventLog], and should not be used anymore if the underlying [EventLog]
   * is modified.
   *
   * @param site the [SiteIdentifier] for which the [EventIterator] is retrieved.
   */
  fun iterator(site: SiteIdentifier): EventIterator

  /**
   * Returns an [EventIterator] specific to a single site. The retrieved [EventIterator] will start
   * at the end of the [EventLog], and should not be used anymore if the underlying [EventLog] is
   * modified.
   *
   * @param site the [SiteIdentifier] for which the [EventIterator] is retrieved.
   */
  fun iteratorAtEnd(site: SiteIdentifier): EventIterator

  /** A listener which may be used to observe some changed performed on an [EventLog]. */
  interface OnLogUpdateListener {

    /** Called when the callback is registered. */
    fun onRegistered() = Unit

    /** Called after the acknowledgements of the [EventLog] are updated. */
    fun onAcknowledgement() = Unit

    /**
     * Called after a new event has been inserted.
     *
     * @param seqno the [SequenceNumber] of the new event.
     * @param site the [SiteIdentifier] of the new event.
     * @param data the [ByteArray] which contains the event.
     * @param from the start indices of the event.
     * @param until the end indices of the event.
     */
    fun onInsert(
        seqno: SequenceNumber,
        site: SiteIdentifier,
        data: ByteArray,
        from: Int,
        until: Int,
    ) = Unit

    /**
     * Called after an event is removed.
     *
     * @param seqno the [SequenceNumber] of the removed event.
     * @param site the [SiteIdentifier] of the removed event.
     */
    fun onRemoved(
        seqno: SequenceNumber,
        site: SiteIdentifier,
    ) = Unit

    /** Called when the log is cleared. */
    fun onCleared() = Unit

    /** Called when the callback is unregistered. */
    fun onUnregistered() = Unit
  }

  /**
   * Registers the provided [OnLogUpdateListener] to this [EventLog].
   *
   * @param listener the [OnLogUpdateListener] which is registered.
   */
  fun registerLogUpdateListener(listener: OnLogUpdateListener)

  /**
   * Unregisters the provided [OnLogUpdateListener] from this [EventLog].
   *
   * @param listener the [OnLogUpdateListener] which is unregistered.
   */
  fun unregisterLogUpdateListener(listener: OnLogUpdateListener)
}

/**
 * Returns true iff there are no events in the [EventLog].
 *
 * @see EventLog.size
 */
fun EventLog.isEmpty(): Boolean {
  return size == 0
}

/**
 * Returns true iff there are some events in the [EventLog].
 *
 * @see EventLog.size
 */
fun EventLog.isNotEmpty(): Boolean {
  return size != 0
}

/** Returns a copy of the current [EventLog]. */
fun EventLog.copyOf(): EventLog = toMutableEventLog()

/** Returns a [MutableEventLog] copy of this [EventLog]. */
fun EventLog.toMutableEventLog(): MutableEventLog {
  return mutableEventLogOf().merge(this)
}

/** Transforms this [EventLog] to a [List] of [Event]. */
fun EventLog.toList(): List<Event> = buildList {
  val iterator = this@toList.iterator().apply { moveToStart() }
  while (iterator.hasNext()) add(iterator.next())
}
