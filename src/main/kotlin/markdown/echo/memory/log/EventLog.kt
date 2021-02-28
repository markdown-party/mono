package markdown.echo.memory.log

import markdown.echo.EchoPreview
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier

/**
 * Creates an empty [EventLog].
 *
 * @param T the type of the events in the log.
 */
@EchoPreview
fun <T> emptyEventLog(): EventLog<T> = EmptyEventLog

/**
 * Creates a new instance of [EventLog],
 *
 * @param events the pairs of event identifiers and event bodies to include in the log.
 *
 * @param T the type of events in the log.
 */
@EchoPreview
fun <T> eventLogOf(
    vararg events: Pair<EventIdentifier, T>,
): EventLog<T> = mutableEventLogOf(*events)

/**
 * Creates a new instance of [MutableEventLog].
 *
 * @param events the pairs of event identifiers and event bodies to include in the log.
 *
 * @param T the type of events in the log.
 */
@EchoPreview
fun <T> mutableEventLogOf(
    vararg events: Pair<EventIdentifier, T>,
): MutableEventLog<T> = SortedMapEventLog(*events)

@EchoPreview
interface EventLog<out T> {

    /**
     * Returns the [SequenceNumber] that is expected from this [EventLog].
     */
    fun expected(site: SiteIdentifier): SequenceNumber

    /**
     * Gets the body of the event with a given [seqno] and [site], if it exists.
     *
     * @param seqno the sequence number of the event.
     * @param site the site of the event.
     */
    operator fun get(
        seqno: SequenceNumber,
        site: SiteIdentifier,
    ): T?
}

@EchoPreview
interface MutableEventLog<T> : EventLog<T> {

    /**
     * Sets the body of the event with a given [seqno] and [site].
     *
     * @param seqno the sequence number of the event.
     * @param site the site of the event.
     * @param body the body of the event.
     */
    operator fun set(
        seqno: SequenceNumber,
        site: SiteIdentifier,
        body: T,
    )
}
