@file:Suppress("FunctionName")

package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.EchoEventLogPreview
import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SequenceNumber.Companion.Zero
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import kotlinx.collections.immutable.*

// TODO : Test this function.
internal fun <T, C> ActualPersistentLog(
    vararg events: Pair<EventIdentifier, T>
): ActualPersistentLog<T, C> =
    events.fold(ActualPersistentLog()) { acc, (id, event) ->
      acc.set(id.site, id.seqno, event, Change.skipped())
    }

/**
 * An implementation of [AbstractEventLog] and [PersistentEventLog] that is backed by some
 * persistent data structures, and optimized for lookups.
 *
 * @param T the type of the stored events.
 */
// TODO : Thoroughly test this class.
internal data class ActualPersistentLog<T, C>
constructor(
    private val all: PersistentList<EventLog.Entry<T, C>>,
    private val bySite:
        PersistentMap<SiteIdentifier, PersistentMap<SequenceNumber, Pair<T, Change<C>>>>,
) : AbstractEventLog<T, C>(), PersistentEventLog<T, C> {

  /** Creates a new [ActualPersistentLog] with no initial content. */
  internal constructor() : this(persistentListOf(), persistentHashMapOf())

  override val sites: Set<SiteIdentifier> = bySite.keys

  @EchoEventLogPreview
  override val expected: SequenceNumber = all.lastOrNull()?.identifier?.seqno?.inc() ?: Zero

  override fun expected(site: SiteIdentifier): SequenceNumber {
    return bySite[site]?.maxOfOrNull { it.key }?.inc() ?: Zero
  }

  override fun get(site: SiteIdentifier, seqno: SequenceNumber): EventLog.Entry<T, C>? {
    return bySite[site]?.get(seqno)?.let {
      EventValueEntry(
          identifier = EventIdentifier(seqno, site),
          body = it.first,
          change = it.second,
      )
    }
  }

  @EchoEventLogPreview
  override fun lastOrNull(): EventLog.Entry<T, C>? {
    return all.lastOrNull()
  }

  override fun events(
      site: SiteIdentifier,
      seqno: SequenceNumber,
  ): Iterable<EventLog.Entry<T, C>> {
    return Iterable { eventIterator(site, seqno) }.filter { it.identifier.site == site }
  }

  override fun eventIterator(
      site: SiteIdentifier,
      seqno: SequenceNumber
  ): EventIterator<EventLog.Entry<T, C>> {
    val identifier = EventIdentifier(seqno, site)
    val startIndex = all.binarySearch { entry -> entry.identifier.compareTo(identifier) }
    val actualIndex = if (startIndex >= 0) startIndex else -(startIndex + 1)
    return Iterator(all.listIterator(actualIndex))
  }

  private inner class Iterator(
      private val backing: ListIterator<EventLog.Entry<T, C>>,
  ) : EventIterator<EventLog.Entry<T, C>> {
    override fun hasPrevious() = backing.hasPrevious()
    override fun previousIndex() = all[backing.previousIndex()].identifier
    override fun previous() = backing.previous()
    override fun hasNext() = backing.hasNext()
    override fun nextIndex() = all[backing.nextIndex()].identifier
    override fun next() = backing.next()
  }

  override fun toPersistentEventLog(): PersistentEventLog<T, C> {
    return this
  }

  override fun set(
      site: SiteIdentifier,
      seqno: SequenceNumber,
      body: T,
      change: Change<C>,
  ): ActualPersistentLog<T, C> {
    val identifier = EventIdentifier(seqno, site)
    val startIndex = all.binarySearch { entry -> entry.identifier.compareTo(identifier) }
    // Duplicate insertions are not supported.
    val actualIndex = if (startIndex < 0) -(startIndex + 1) else return this

    val newAll = all.mutate { it.add(actualIndex, EventValueEntry(identifier, body, change)) }
    val newBySite =
        bySite.mutate {
          val siteMap = it.getOrPut(site) { persistentHashMapOf() }
          it[site] = siteMap.mutate { map -> map[seqno] = Pair(body, change) }
        }

    return ActualPersistentLog(
        all = newAll,
        bySite = newBySite,
    )
  }

  override fun remove(
      site: SiteIdentifier,
      seqno: SequenceNumber,
  ): ActualPersistentLog<T, C> {
    val newAll = all.removeAll { it.identifier == EventIdentifier(seqno, site) }
    val siteMap = bySite[site]?.remove(seqno)
    val newBySite = siteMap?.let { bySite.put(site, it) } ?: bySite

    return ActualPersistentLog(
        all = newAll,
        bySite = newBySite,
    )
  }
}
