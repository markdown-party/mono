@file:OptIn(
    EchoPreview::class,
    ExperimentalCoroutinesApi::class,
    FlowPreview::class,
    InternalCoroutinesApi::class,
)

package markdown.echo.memory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import markdown.echo.Echo
import markdown.echo.EchoPreview
import markdown.echo.Message
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SequenceNumber.Companion.Zero
import markdown.echo.causal.SiteIdentifier
import markdown.echo.channelExchange
import markdown.echo.events.SiteSendEcho
import markdown.echo.memory.log.EventLog
import markdown.echo.memory.log.MutableEventLog
import markdown.echo.memory.log.mutableEventLogOf
import markdown.echo.Message.V1.Incoming as I
import markdown.echo.Message.V1.Outgoing as O

/**
 * Creates a new [MemoryEcho] instance, that uses a memory-backed store for the events. This [Echo]
 * has a unique identifier, and considers that it is the unique issuer of the events for this
 * specific site identifier.
 *
 * This [Echo] offers bi-directional syncing capabilities, with one-off and continuous sync. It
 * issues push updates when the events in the memory store are modified by a different site.
 *
 * @param site the unique identifier for the site of this [Echo].
 * @param log the [MutableEventLog] backing this [MemoryEcho].
 *
 * @param T the type of the domain-specific events that this [MemoryEcho] supports.
 */
fun <T> Echo.Companion.memory(
    site: SiteIdentifier,
    log: MutableEventLog<T> = mutableEventLogOf(),
): MemoryEcho<T> = MemoryEcho(site, log)

/**
 * An implementation of [Echo] and [SiteSendEcho] that uses an in-memory [MutableEventLog], and
 * has the authority to issue operations for a given [site].
 *
 * @param site the unique identifier for the site of this [Echo].
 *
 * @param T the type of the domain-specific events that this [MemoryEcho] supports.
 */
@OptIn(
    EchoPreview::class,
    ExperimentalCoroutinesApi::class,
    InternalCoroutinesApi::class,
    FlowPreview::class,
)
class MemoryEcho<T>(
    override val site: SiteIdentifier,
    private val log: MutableEventLog<T> = mutableEventLogOf(),
) : Echo<I<T>, O<T>>, SiteSendEcho<I<T>, O<T>> {

    // TODO : Handle variable request size.

    private val mutex = Mutex()
    private val lastInserted = MutableStateFlow<EventIdentifier?>(null)

    override fun outgoing() = channelExchange<I<T>, O<T>> { incoming ->
        // TODO : Support outgoing exchanges.
        var state: OutgoingState<T> = OutgoingState.Advertising(mutableListOf())

        while (state != OutgoingState.Completed && !(incoming.isClosedForReceive && isClosedForSend)) {
            state = when (val s = state) {

                is OutgoingState.Advertising -> select {
                    incoming.onReceiveOrClosed { v ->
                        when (val msg = v.valueOrNull) {
                            is I.Advertisement -> {
                                s.availableSites += msg.site
                                return@onReceiveOrClosed s // Updated a mutable state.
                            }
                            is I.Ready -> {
                                OutgoingState.Listening(
                                    pendingRequests = s.availableSites,
                                    requested = mutableListOf(),
                                )
                            }
                            is I.Done, null -> OutgoingState.Cancelling
                            is I.Event -> error("Expected Advertisement or Ready.")
                        }
                    }
                }

                is OutgoingState.Listening -> {
                    val request = s.pendingRequests.lastOrNull()
                    val expected = mutex.withLock { request?.let(log::expected) } ?: Zero
                    select {
                        if (request != null) {
                            onSend(O.Request(
                                seqno = expected,
                                site = request,
                                count = Long.MAX_VALUE,
                            )) {
                                s.pendingRequests.removeLast()
                                s.requested.add(request)
                                return@onSend s // Updated a mutable state.
                            }
                        }
                        incoming.onReceiveOrClosed { v ->
                            when (val msg = v.valueOrNull) {
                                is I.Done, null -> OutgoingState.Cancelling
                                is Message.V1.Incoming.Advertisement -> {
                                    s.pendingRequests.add(msg.site)
                                    return@onReceiveOrClosed s // Updated a mutable state.
                                }
                                is Message.V1.Incoming.Event -> {
                                    mutex.withLock {
                                        val present = log[msg.seqno, msg.site] != null
                                        if (!present) {
                                            log[msg.seqno, msg.site] = msg.body
                                            lastInserted.value = EventIdentifier(
                                                msg.seqno,
                                                msg.site,
                                            )
                                        }
                                    }
                                    return@onReceiveOrClosed s // No-Op on the local state.
                                }
                                is Message.V1.Incoming.Ready -> error("Duplicate Ready message.")
                            }
                        }
                    }
                }

                // 1. We can send a Done message, and move to Completed.
                is OutgoingState.Cancelling -> select {
                    onSend(O.Done) { OutgoingState.Completed }
                }

                // 1. We receive a Done message, and move to Completed.
                is OutgoingState.Completing -> select {
                    incoming.onReceiveOrClosed { v ->
                        when (v.valueOrNull) {
                            is I.Done, null -> OutgoingState.Completed
                            // TODO : Eventually store these "free" events ?
                            else -> OutgoingState.Completing // Draining.
                        }
                    }
                }

                // This should never be called. It's included for completeness.
                is OutgoingState.Completed -> OutgoingState.Completed
            }
        }
    }

    override fun incoming() = channelExchange<O<T>, I<T>> { incoming ->
        // TODO : Support incoming exchanges.
        val insertion = lastInserted.buffer(Channel.RENDEZVOUS).produceIn(this)
        var state: IncomingState<T> = mutex.withLock {
            IncomingState.New(
                advertisedSites = mutableListOf(),
                pendingSites = log.sites.toMutableList(),
            )
        }

        while (state != IncomingState.Completed && !(incoming.isClosedForReceive && isClosedForSend)) {
            state = when (val s = state) {

                // 1. We have some pending advertisements to send before we can issue the Ready, so
                //    we simply issue them and move them from our pending queue to the advertised
                //    queue.
                // 2. We have advertised all of our pending sites, so we can issue a Ready event and
                //    move to the Sending state.
                // 3. We receive a Done event, so we move to Cancelling.
                // 4. We receive an unsupported message, which we just ignore. // TODO : Fail fast instead ?
                is IncomingState.New -> select {
                    val pending = s.pendingSites.lastOrNull()
                    if (pending != null) {
                        onSend(I.Advertisement(pending)) {
                            s.advertisedSites.add(pending)
                            s.pendingSites.removeLast()
                            return@onSend s // Updated a mutable state.
                        }
                    } else {
                        onSend(I.Ready) {
                            mutex.withLock {
                                IncomingState.Sending<T>(
                                    advertisedSites = s.advertisedSites,
                                    pendingEvents = emptyList(),
                                    pendingSites = emptyList(),
                                    receivedAcks = emptyMap(),
                                    receivedCredits = emptyMap(),
                                ).update(log)
                            }
                        }
                    }
                    incoming.onReceiveOrClosed { v ->
                        when (v.valueOrNull) {
                            O.Done, null -> IncomingState.Cancelling
                            else -> s // NoOp.
                        }
                    }
                }

                is IncomingState.Sending -> select {

                    // Highest priority, generally, is sending events that we may have in the queue.
                    // Each exchange can therefore work without interrupting other exchanges.
                    val event =
                        s.pendingEvents.firstOrNull { (id, _) -> s.receivedCredits[id.site] ?: 0L > 0L }
                    if (event != null) {
                        onSend(I.Event(
                            site = event.first.site,
                            seqno = event.first.seqno,
                            body = event.second
                        )) {
                            // Diminish credits by one, ack a new operation and
                            val creditsForSite = s.receivedCredits[event.first.site] ?: 0L
                            val ackForSite = s.receivedAcks[event.first.site] ?: event.first.seqno
                            val newCredits =
                                s.receivedCredits + (event.first.site to creditsForSite - 1)
                            val newAcks = s.receivedAcks + (event.first.site to maxOf(
                                event.first.seqno,
                                ackForSite,
                            ))
                            val newEvents = s.pendingEvents - event
                            IncomingState.Sending(
                                advertisedSites = s.advertisedSites,
                                pendingEvents = newEvents,
                                pendingSites = s.pendingSites,
                                receivedAcks = newAcks,
                                receivedCredits = newCredits,
                            )
                        }
                    }
                    val firstSite = s.pendingSites.firstOrNull()
                    if (firstSite != null) {
                        onSend(I.Advertisement(firstSite)) {
                            mutex.withLock {
                                IncomingState.Sending(
                                    advertisedSites = s.advertisedSites.plus(firstSite),
                                    pendingEvents = s.pendingEvents,
                                    pendingSites = s.pendingSites.drop(1),
                                    receivedAcks = s.receivedAcks,
                                    receivedCredits = s.receivedCredits,
                                ).update(log)
                            }
                        }
                    }
                    incoming.onReceiveOrClosed { v ->
                        when (val msg = v.valueOrNull) {
                            is O.Request -> {
                                // Ack based on request, and set credits for the given site.
                                val ackForSite =
                                    maxOf(s.receivedAcks[msg.site] ?: msg.seqno, msg.seqno)
                                val newAcks = s.receivedAcks + (msg.site to ackForSite)
                                val newCredits = s.receivedCredits + (msg.site to msg.count)
                                IncomingState.Sending(
                                    advertisedSites = s.advertisedSites,
                                    pendingEvents = s.pendingEvents,
                                    pendingSites = s.pendingSites,
                                    receivedAcks = newAcks,
                                    receivedCredits = newCredits,
                                )
                            }
                            is O.Done, null -> IncomingState.Cancelling
                        }
                    }
                    insertion.onReceive { _ ->
                        mutex.withLock { s.update(log) }
                    }
                }

                // 1. We can send a Done message, and move to Completed.
                is IncomingState.Cancelling -> select {
                    onSend(I.Done) { IncomingState.Completed }
                }

                // 1. We receive a Done message, and move to Completed.
                is IncomingState.Completing -> select {
                    incoming.onReceiveOrClosed { v ->
                        when (v.valueOrNull) {
                            is O.Done, null -> IncomingState.Completed
                            is O.Request -> IncomingState.Completing // Draining.
                        }
                    }
                }

                // This should never be called. It's included for completeness.
                is IncomingState.Completed -> IncomingState.Completed
            }
        }

        // Cancel the different coroutines.
        incoming.cancel()
        insertion.cancel()
    }
}

@EchoPreview
private sealed class OutgoingState<out T> {
    data class Advertising(
        val availableSites: MutableList<SiteIdentifier>,
    ) : OutgoingState<Nothing>()

    data class Listening(
        val pendingRequests: MutableList<SiteIdentifier>,
        val requested: MutableList<SiteIdentifier>,
    ) : OutgoingState<Nothing>()

    object Cancelling : OutgoingState<Nothing>()
    object Completing : OutgoingState<Nothing>()
    object Completed : OutgoingState<Nothing>()
}

@EchoPreview
private sealed class IncomingState<out T> {

    data class New(
        val advertisedSites: MutableList<SiteIdentifier>,
        val pendingSites: MutableList<SiteIdentifier>,
    ) : IncomingState<Nothing>()

    // TODO : Optimize with mutable states.
    data class Sending<T>(
        val advertisedSites: List<SiteIdentifier>,
        val pendingEvents: List<Pair<EventIdentifier, T>>,
        val pendingSites: List<SiteIdentifier>,
        val receivedAcks: Map<SiteIdentifier, SequenceNumber>,
        val receivedCredits: Map<SiteIdentifier, Long>,
    ) : IncomingState<T>() {

        /**
         * Uses the [EventLog] to update the [Sending] state with missing pending sites and missing
         * pending events.
         *
         * @param log the [EventLog] that's used to update the [Sending] state.
         */
        fun update(
            log: EventLog<T>,
        ): Sending<T> {
            val newSites = log.sites - advertisedSites
            val newEvents = advertisedSites
                .asSequence().flatMap { site ->
                    log.events(
                        seqno = receivedAcks[site] ?: SequenceNumber.Zero,
                        site = site,
                    )
                }
                .toList()

            return Sending(
                advertisedSites = advertisedSites,
                pendingEvents = newEvents,
                pendingSites = newSites.toList(),
                receivedAcks = receivedAcks,
                receivedCredits = receivedCredits,
            )
        }
    }

    object Cancelling : IncomingState<Nothing>()
    object Completing : IncomingState<Nothing>()
    object Completed : IncomingState<Nothing>()
}
