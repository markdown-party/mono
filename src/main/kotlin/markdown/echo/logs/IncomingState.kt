@file:OptIn(InternalCoroutinesApi::class)
@file:Suppress("SameParameterValue")

package markdown.echo.logs

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.selects.select
import markdown.echo.Message.V1.Incoming as Inc
import markdown.echo.Message.V1.Outgoing as Out
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier

/**
 * A sealed class representing the different states that the finite state machine may be in. Each
 * step may decide to continue or stop processing with the [keepGoing] method, and defines state
 * transitions as part of the [step].
 *
 * @param T the type of the events.
 */
// TODO : Update the documentation.
internal sealed class IncomingState<T> : State<Out<T>, Inc<T>, T, IncomingState<T>> {

  companion object {

    /** Creates a new [IncomingState] that's the beginning of the FSM. */
    operator fun <T> invoke(pending: Iterable<SiteIdentifier>): IncomingState<T> =
        IncomingNew(
            advertisedSites = mutableListOf(),
            pendingSites = pending.toMutableList(),
        )
  }
}

// FINITE STATE MACHINE

// 1. We have some pending advertisements to send before we can issue the Ready, so
//    we simply issue them and move them from our pending queue to the advertised
//    queue.
// 2. We have advertised all of our pending sites, so we can issue a Ready event and
//    move to the Sending state.
// 3. We receive a Done event, so we move to Cancelling.
// 4. We receive an unsupported message, which we just ignore. // TODO : Fail fast instead ?
private data class IncomingNew<T>(
    private val advertisedSites: MutableList<SiteIdentifier>,
    private val pendingSites: MutableList<SiteIdentifier>,
) : IncomingState<T>() {

  override suspend fun IncomingStepScope<T>.step(
      log: ImmutableEventLog<T>
  ): Effect<IncomingState<T>> {
    return select {
      val pending = pendingSites.lastOrNull()
      if (pending != null) {
        onSend(Inc.Advertisement(pending)) {
          advertisedSites.add(pending)
          pendingSites.removeLast()
          Effect.Move(this@IncomingNew) // mutable state updated.
        }
      } else {
        onSend(Inc.Ready) {
          Effect.Move(
              IncomingSending<T>(
                      advertisedSites = advertisedSites,
                      pendingEvents = emptyList(),
                      pendingSites = emptyList(),
                      receivedAcks = emptyMap(),
                      receivedCredits = emptyMap(),
                  )
                  .update(log))
        }
      }
      onReceiveOrClosed { v ->
        when (v.valueOrNull) {
          Out.Done, null -> Effect.Move(IncomingCancelling())
          else -> Effect.Move(this@IncomingNew) // NoOp. TODO : Handle things differently ?
        }
      }
    }
  }
}

// TODO : Optimize with mutable states.
private data class IncomingSending<T>(
    private val advertisedSites: List<SiteIdentifier>,
    private val pendingEvents: List<Pair<EventIdentifier, T>>,
    private val pendingSites: List<SiteIdentifier>,
    private val receivedAcks: Map<SiteIdentifier, SequenceNumber>,
    private val receivedCredits: Map<SiteIdentifier, Long>,
) : IncomingState<T>() {

  /**
   * Uses the [EventLog] to update the [Sending] state with missing pending sites and missing
   * pending events.
   *
   * @param log the [EventLog] that's used to update the [Sending] state.
   */
  fun update(
      log: ImmutableEventLog<T>,
  ): IncomingSending<T> {
    val newSites = log.sites - advertisedSites
    val newEvents =
        advertisedSites
            .asSequence()
            .flatMap { site ->
              log.events(
                  seqno = receivedAcks[site] ?: SequenceNumber.Zero,
                  site = site,
              )
            }
            .toList()

    return IncomingSending(
        advertisedSites = advertisedSites,
        pendingEvents = newEvents,
        pendingSites = newSites.toList(),
        receivedAcks = receivedAcks,
        receivedCredits = receivedCredits,
    )
  }

  override suspend fun IncomingStepScope<T>.step(
      log: ImmutableEventLog<T>
  ): Effect<IncomingState<T>> {
    return select {
      // Highest priority, generally, is sending events that we may have in the
      // queue.
      // Each exchange can therefore work without interrupting other exchanges.
      val event = pendingEvents.firstOrNull { (id, _) -> receivedCredits[id.site] ?: 0L > 0L }
      if (event != null) {
        onSend(Inc.Event(site = event.first.site, seqno = event.first.seqno, body = event.second)) {
          // Diminish credits by one, ack a new operation and update the state.
          val creditsForSite = receivedCredits[event.first.site] ?: 0L
          val ackForSite = receivedAcks[event.first.site] ?: event.first.seqno
          val newCredits = receivedCredits + (event.first.site to creditsForSite - 1)
          val newAcks = receivedAcks + (event.first.site to maxOf(event.first.seqno, ackForSite))
          val newEvents = pendingEvents - event
          Effect.Move(
              IncomingSending(
                  advertisedSites = advertisedSites,
                  pendingEvents = newEvents,
                  pendingSites = pendingSites,
                  receivedAcks = newAcks,
                  receivedCredits = newCredits,
              ))
        }
      }
      val firstSite = pendingSites.firstOrNull()
      if (firstSite != null) {
        onSend(Inc.Advertisement(firstSite)) {
          Effect.Move(
              IncomingSending(
                      advertisedSites = advertisedSites.plus(firstSite),
                      pendingEvents = pendingEvents,
                      pendingSites = pendingSites.drop(1),
                      receivedAcks = receivedAcks,
                      receivedCredits = receivedCredits,
                  )
                  .update(log))
        }
      }
      onReceiveOrClosed { v ->
        when (val msg = v.valueOrNull) {
          is Out.Request -> {
            // Ack based on request, and set credits for the given site.
            val ackForSite = maxOf(receivedAcks[msg.site] ?: msg.nextForSite, msg.nextForSite)
            val newAcks = receivedAcks + (msg.site to ackForSite)
            val newCredits = receivedCredits + (msg.site to msg.count)
            Effect.Move(
                copy(
                    receivedAcks = newAcks,
                    receivedCredits = newCredits,
                ))
          }
          is Out.Done, null -> Effect.Move(IncomingCancelling())
        }
      }
      onInsert { new -> Effect.Move(update(new)) }
    }
  }
}

// 1. We can send a Done message, and move to Completed.
private class IncomingCancelling<T> : IncomingState<T>() {

  override suspend fun IncomingStepScope<T>.step(
      log: ImmutableEventLog<T>
  ): Effect<IncomingState<T>> {
    return select { onSend(Inc.Done) { Effect.Terminate } }
  }
}
