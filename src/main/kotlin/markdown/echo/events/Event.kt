@file:OptIn(
    ExperimentalCoroutinesApi::class,
    InternalCoroutinesApi::class,
)

package markdown.echo.events

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.selects.select
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.events.State.*
import markdown.echo.Message.V1.Incoming as I
import markdown.echo.Message.V1.Outgoing as O

/**
 * A sealed class representing the possible states for the event { ... } scope. See the [event]
 * implementation for details on the different states.
 *
 * @param T the type of the domain-specific events to add.
 */
private sealed class State<out T> {
    object New : State<Nothing>()
    object WaitingAdvertisement : State<Nothing>()
    object WaitingReady : State<Nothing>()

    data class Sending<T>(
        val remaining: List<Pair<EventIdentifier, T>>,
        val allowed: Long, // How many items we're still allowed to send.
    ) : State<T>()

    object Yielding : State<Nothing>()
    object Cancelling : State<Nothing>()
    object Completing : State<Nothing>()
    object Completed : State<Nothing>()
}

/**
 * Creates some new events, that are generated in the [EventScope]. This function returns once the
 * events have been successfully added to the underlying [SiteSendEcho].
 *
 * @param T the type of the domain-specific events to add.
 */
suspend fun <T> SiteSendEcho<I<T>, O<T>>.event(
    scope: suspend EventScope<T>.() -> Unit,
): Unit = coroutineScope {

    // State machine transitions for the event { ... } issuer. Messages that are sent to the
    // SiteSendEcho point to right (->), and messages received from the SiteSendEcho point to
    // the left (<-).
    //
    //
    // +-----+  -> Advertisement & Ready   +---------+  <- Request  +---------+
    // | NEW | --------------------------> | WAITING | ------------ | SENDING |
    // +-----+                             +---------+              +---------+
    //    | <- Done                            |                      |  |  -> Events
    //    V                                    | <- Done      --------+  V
    // +------------+                          |              |     +----------+
    // | CANCELLING | <------------------------+--------------+-----| YIELDING | <-----+
    // +------------+                                <- Done        +----------+       |
    //    | -> Done                                                    |   |           |
    //    V                                                            |   +-----------+
    // +-----------+                    +------------+                 |     <- Request
    // | COMPLETED | <------------------| COMPLETING |-----------------+
    // +-----------+       <- Done      +------------+      -> Done
    //                                     |      ^
    //                                     |      |
    //                                     +------+
    //                                    <- Request
    //
    //
    // Once the COMPLETED state is reached, channels are closed and the connection with the
    // other side is terminated.

    // Start up a sender coroutine, with a dedicated channel for incoming messages.
    val incoming = Channel<O<T>>()
    val outgoing = produce {

        // Iterate on the State until we are completed.
        var state: State<T> = New
        while (state != Completed && !(isClosedForSend && incoming.isClosedForReceive)) {
            state = when (val s = state) {

                // 1. We can send an advertisement and move to WaitingAdvertisement.
                // 2. We can receive a Done message and move to Cancelling.
                is New -> select {
                    onSend(I.Advertisement(site)) { WaitingAdvertisement }
                    incoming.onReceiveOrClosed { v ->
                        if (v.isClosed || v.valueOrNull == O.Done) Cancelling
                        else New
                    }
                }

                // 1. We can send a ready and move to WaitingReady.
                // 2. We can receive a Request message, and move to Sending if we have some events
                //    to send. Otherwise, we may skip directly to Yielding if the event { ... }
                //    scope generated no events.
                // 3. We can receive a Done message and move to Cancelling.
                is WaitingAdvertisement -> select {
                    onSend(I.Ready) { WaitingReady }
                    incoming.onReceiveOrClosed { v ->
                        when (val msg = v.valueOrNull) {
                            is O.Request ->
                                if (msg.site == site && msg.count > 0) {
                                    val remaining = events(site, msg.nextForAll, scope)
                                    if (remaining.isNotEmpty()) Sending(remaining, msg.count)
                                    else Yielding
                                } else WaitingReady
                            is O.Done -> Cancelling
                            null -> Cancelling
                        }
                    }
                }

                // 1. We can receive a request for the advertised site. If we are asked to issue
                //    some messages and have some events to send, we move to Sending. Otherwise,
                //    we move to Yielding.
                // 2. We can receive a request for another site. We stay at WaitingReady.
                // 3. We can receive a Done message, and move to Cancelling.
                is WaitingReady -> select {
                    incoming.onReceiveOrClosed { v ->
                        when (val msg = v.valueOrNull) {
                            is O.Request ->
                                if (msg.site == site && msg.count > 0) {
                                    val remaining = events(site, msg.nextForAll, scope)
                                    if (remaining.isNotEmpty()) Sending(remaining, msg.count)
                                    else Yielding
                                } else WaitingReady
                            is O.Done -> Cancelling
                            null -> Cancelling
                        }
                    }
                }

                // 1. We have at least one permit (from backpressure), so we can send one of the
                //    events of the scope. If the event is send, we either move to Sending with one
                //    less event and one less permit, or move to Yielding if we're done sending
                //    everything.
                // 2. We can receive a request with more permits for the same site. We add them to
                //    our Sending state.
                // 3. We can receive a request for another site, and just ignore it.
                // 4. We can receive a Done message, and move to Cancelling.
                is Sending -> select {
                    val (id, body) = s.remaining[0]
                    val (seqno, site) = id
                    if (s.allowed >= 1) {
                        onSend(I.Event(site = site, seqno = seqno, body = body)) {
                            val remaining = s.remaining.drop(1)
                            if (remaining.isNotEmpty()) Sending(remaining, s.allowed - 1)
                            else Yielding
                        }
                    }
                    incoming.onReceiveOrClosed { v ->
                        when (val msg = v.valueOrNull) {
                            is O.Request -> {
                                if (msg.site == site) Sending(s.remaining, msg.count)
                                else Sending(s.remaining, s.allowed)
                            }
                            is O.Done -> Cancelling
                            null -> Cancelling
                        }
                    }
                }

                // 1. We can receive a request, and just ignore it.
                // 2. We can receive a Done message, and move to cancelling.
                // 3. We can send a Done message, and move to Completing.
                is Yielding -> select {
                    incoming.onReceiveOrClosed { v ->
                        when (v.valueOrNull) {
                            is O.Request -> Yielding
                            is O.Done -> Cancelling
                            null -> Cancelling
                        }
                    }
                    onSend(I.Done) { Completing }
                }

                // We can send a Done message, and move to Completed.
                is Cancelling -> select {
                    onSend(I.Done) { Completed }
                }

                // 1. We can receive a Done message, and move to Completed.
                is Completing -> select {
                    incoming.onReceiveOrClosed { v ->
                        if (v.isClosed || v.valueOrNull == O.Done) Completed
                        else Completing
                    }
                }

                // This should never be called. It's included for completeness.
                is Completed -> Completed
            }
        }
    }

    // Start the exchange between the sites.
    outgoing().talk(outgoing.consumeAsFlow())
        .onEach { incoming.send(it) }
        .onCompletion { incoming.close() }
        .collect()
}

/**
 * Executes the [scope] block in a suspending fashion, returning a [List] of the [EventIdentifier]
 * and [T] pairs for each of the event that yielded in the scope.
 *
 * @param site the site for the events.
 * @param start the first sequence number for the events.
 * @param scope the scope to execute.
 */
private suspend fun <T> events(
    site: SiteIdentifier,
    start: SequenceNumber,
    scope: suspend EventScope<T>.() -> Unit,
): List<Pair<EventIdentifier, T>> {
    var next = start
    val items = mutableListOf<Pair<EventIdentifier, T>>()
    val impl = object : EventScope<T> {
        override suspend fun yield(event: T): EventIdentifier {
            val identifier = EventIdentifier(next++, site)
            items += identifier to event
            return identifier
        }
    }
    scope(impl)
    return items
}
