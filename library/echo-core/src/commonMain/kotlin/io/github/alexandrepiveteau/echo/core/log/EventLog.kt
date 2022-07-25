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

  /** A listener which may be used to observe some changed performed on an [EventLog]. */
  fun interface OnLogUpdateListener {

    /** A callback which will be called whenever some updates are performed on an [EventLog]. */
    fun onLogUpdated()
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

/** Transforms this [EventLog] to a [List] of [Event]. */
fun EventLog.toList(): List<Event> = buildList {
  val iterator = this@toList.iterator().apply { moveToStart() }
  while (iterator.hasNext()) add(iterator.next())
}
