package markdown.echo.logs

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import markdown.echo.EchoEventLogPreview
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SequenceNumber.Companion.Zero
import markdown.echo.causal.SiteIdentifier

/**
 * An implementation of a [PersistentEventLog] that uses persistent data structures.
 *
 * @param buffer the backing [PersistentMap] data structure.
 *
 * @param T the type of the body of the events.
 */
internal class PersistentMapEventLog<T>
internal constructor(
    private val buffer: PersistentMap<SiteIdentifier, PersistentMap<SequenceNumber, T>>,
) : PersistentEventLog<T> {

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
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): T? = buffer[site]?.get(seqno)

  override fun events(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ): Iterable<Pair<EventIdentifier, T>> {
    return buffer
        .getOrElse(site) { sortedMapOf() }
        .asSequence()
        .filter { it.key >= seqno }
        .asSequence()
        .map { (seqno, body) -> EventIdentifier(seqno, site) to body }
        .asIterable()
  }

  override operator fun set(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      body: T,
  ): PersistentEventLog<T> {
    val existing = buffer.getOrElse(site, ::persistentMapOf)
    val updated = existing.put(seqno, body)
    return PersistentMapEventLog(buffer.put(site, updated))
  }

  @EchoEventLogPreview
  override fun <R> foldl(
      base: R,
      step: (Pair<EventIdentifier, T>, R) -> R,
  ): R =
      buffer
          .asSequence()
          .flatMap { entry ->
            entry.value.asSequence().map {
              val id = EventIdentifier(it.key, entry.key)
              val body = it.value
              Pair(id, body)
            }
          }
          .sortedBy { it.first }
          .fold(base) { m, p -> step(p, m) }

  override fun toPersistentEventLog(): PersistentEventLog<T> = this

  override fun toString(): String {
    return "SortedMapEventLog(buffer: $buffer)"
  }
}
