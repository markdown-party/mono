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
     * Returns an [Iterable] of all the [SiteIdentifier] that are known to this [EventLog].
     */
    val sites: Set<SiteIdentifier>

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

    /**
     * Returns all the events greater or equal to the provided [SequenceNumber] for the given
     * [SiteIdentifier].
     *
     * @param seqno the lowest sequence number of the expected events.
     * @param site the site of the event.
     *
     * @return all the events that are equal or higher to this [seqno] for the [site].
     *
     * TODO : Use a custom return type ?
     */
    fun events(
        seqno: SequenceNumber,
        site: SiteIdentifier,
    ): Iterable<Pair<EventIdentifier, T>>
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
