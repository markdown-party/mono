@file:OptIn(EchoPreview::class)

package markdown.echo.memory.log

import markdown.echo.EchoPreview
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier

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
    private val buffer = sortedMapOf<EventIdentifier, T>()

    /**
     * A table that stores the last acknowledge event for each [SiteIdentifier]. This is updated
     * through the [ack] method.
     */
    private val table = mutableMapOf<SiteIdentifier, SequenceNumber>()

    init {
        for ((key, value) in events) {
            this[key.seqno, key.site] = value
        }
    }

    /**
     * Acknowledges the provided event identifier as known by this [MutableEventLog].
     */
    private fun ack(
        event: EventIdentifier,
    ) {
        val current = table[event.site]
        val next =
            if (current == null) event.seqno
            else maxOf(current, event.seqno)

        table[event.site] = next
    }

    override val sites: Set<SiteIdentifier>
        get() = table.keys

    override fun expected(
        site: SiteIdentifier,
    ) = table[site]?.inc() ?: SequenceNumber.Zero

    override fun get(
        seqno: SequenceNumber,
        site: SiteIdentifier,
    ): T? = buffer[EventIdentifier(seqno, site)]

    override fun events(
        seqno: SequenceNumber,
        site: SiteIdentifier,
    ): Iterable<Pair<EventIdentifier, T>> = buffer.asSequence()
        .filter { (id, _) -> id.site == site }
        .filter { (id, _) -> id.seqno >= seqno }
        .sortedBy { (id, _) -> id }
        .map { (id, body) -> id to body }
        .asIterable()

    override operator fun set(
        seqno: SequenceNumber,
        site: SiteIdentifier,
        body: T,
    ) {
        with(EventIdentifier(seqno, site)) {
            ack(this)
            buffer[this] = body
        }
    }
}
