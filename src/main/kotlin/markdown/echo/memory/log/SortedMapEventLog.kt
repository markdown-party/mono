package markdown.echo.memory.log

import markdown.echo.EchoEventLogPreview
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import java.util.*

/**
 * An implementation of a [MutableEventLog] that makes use of [java.util.SortedMap].
 *
 * @param T the type of the body of the events.
 */
internal class SortedMapEventLog<T> internal constructor(
    vararg events: Pair<EventIdentifier, T>,
) : MutableEventLog<T> {

    /**
     * The sorted structure that associates event identifiers to an operation body. Because event
     * identifiers are totally ordered, it's possible to efficient iterate on the events from the
     * [MutableEventLog].
     */
    private val buffer = mutableMapOf<SiteIdentifier, SortedMap<SequenceNumber, T>>()

    init {
        for ((key, value) in events) {
            this[key.seqno, key.site] = value
        }
    }

    override val sites: Set<SiteIdentifier>
        get() = buffer.keys

    @EchoEventLogPreview
    override val expected: SequenceNumber
        // TODO : Optimize this to be constant time.
        get() = buffer.values.maxOfOrNull { it.lastKey() + 1U } ?: SequenceNumber.Zero

    override fun expected(
        site: SiteIdentifier,
    ) = buffer[site]?.lastKey()?.inc() ?: SequenceNumber.Zero

    override fun get(
        seqno: SequenceNumber,
        site: SiteIdentifier,
    ): T? = buffer[site]?.get(seqno)

    override fun events(
        seqno: SequenceNumber,
        site: SiteIdentifier,
    ): Iterable<Pair<EventIdentifier, T>> {
        // TODO : Change implementation so read-only events() does not mutate [buffer]
        return buffer.getOrPut(site) { sortedMapOf() }
            .tailMap(seqno)
            .asSequence()
            .map { (seqno, body) -> EventIdentifier(seqno, site) to body }
            .asIterable()
    }

    override operator fun set(
        seqno: SequenceNumber,
        site: SiteIdentifier,
        body: T,
    ) {
        buffer.getOrPut(site) { sortedMapOf() }[seqno] = body
    }

    @EchoEventLogPreview
    override fun <R> foldl(
        base: R,
        step: (Pair<EventIdentifier, T>, R) -> R,
    ): R = buffer.asSequence()
        .flatMap { entry ->
            entry.value.asSequence()
                .map {
                    val id = EventIdentifier(it.key, entry.key)
                    val body = it.value
                    Pair(id, body)
                }
        }
        .sortedBy { it.first }
        .fold(base) { m, p -> step(p, m) }
}
