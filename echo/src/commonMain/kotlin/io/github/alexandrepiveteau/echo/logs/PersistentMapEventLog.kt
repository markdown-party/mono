package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.EchoEventLogPreview
import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SequenceNumber.Companion.Zero
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * An implementation of a [PersistentEventLog] that uses persistent data structures.
 *
 * @param buffer the backing [PersistentMap] data structure.
 *
 * @param T the type of the body of the events.
 */
internal data class PersistentMapEventLog<T>
internal constructor(
    private val buffer: PersistentMap<SiteIdentifier, PersistentMap<SequenceNumber, T>>,
) : PersistentEventLog<T>, AbstractEventLog<T>() {

  constructor(
      vararg events: Pair<EventIdentifier, T>,
  ) : this(
      events.fold(
          persistentMapOf<SiteIdentifier, PersistentMap<SequenceNumber, T>>(),
      ) { acc, (id, event) ->
        val existing = acc.getOrElse(id.site, ::persistentMapOf)
        val updated = existing.put(id.seqno, event)
        acc.put(id.site, updated)
      },
  )

  override val sites: Set<SiteIdentifier>
    get() = buffer.keys

  @EchoEventLogPreview
  override val expected: SequenceNumber
    // TODO : Optimize this to be constant time.
    get() = buffer.keys.maxOfOrNull { expected(it) } ?: Zero

  override fun expected(
      site: SiteIdentifier,
  ) = buffer[site]?.maxOfOrNull { it.key }?.inc() ?: Zero

  override fun get(
      site: SiteIdentifier,
      seqno: SequenceNumber,
  ): EventLog.Entry<T>? =
      buffer[site]?.get(seqno)?.let { body -> EventValueEntry(EventIdentifier(seqno, site), body) }

  override fun events(
      site: SiteIdentifier,
      seqno: SequenceNumber,
  ): Iterable<EventLog.Entry<T>> {
    return buffer
        .getOrElse(site) { emptyMap() }
        .asSequence()
        .filter { it.key >= seqno }
        .asSequence()
        .map { (seqno, body) -> EventValueEntry(EventIdentifier(seqno, site), body) }
        .asIterable()
  }

  override operator fun set(
      site: SiteIdentifier,
      seqno: SequenceNumber,
      body: T,
  ): PersistentEventLog<T> {
    val existing = buffer.getOrElse(site, ::persistentMapOf)
    val updated = existing.put(seqno, body)
    return PersistentMapEventLog(buffer.put(site, updated))
  }

  override fun remove(
      site: SiteIdentifier,
      seqno: SequenceNumber,
  ): PersistentEventLog<T> {
    val existing = buffer[site] ?: return this
    val updated = existing.remove(seqno)
    return PersistentMapEventLog(buffer.put(site, updated))
  }

  override fun toPersistentEventLog(): PersistentEventLog<T> = this

  override fun toString(): String {
    return "SortedMapEventLog(buffer: $buffer)"
  }
}
