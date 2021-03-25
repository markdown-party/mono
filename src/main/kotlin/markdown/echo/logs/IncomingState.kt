@file:OptIn(InternalCoroutinesApi::class)
@file:Suppress("SameParameterValue")

package markdown.echo.logs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.withLock
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
internal sealed class IncomingState<T> {

  companion object {

    /** Creates a new [IncomingState] that's the beginning of the FSM. */
    operator fun <T> invoke(pending: Iterable<SiteIdentifier>): IncomingState<T> =
        IncomingNew(
            advertisedSites = mutableListOf(),
            pendingSites = pending.toMutableList(),
        )
  }

  /**
   * Returns true iff a loop on this state should keep iterating.
   *
   * @param incoming the [ReceiveChannel] for events coming from other sites.
   * @param outgoing the [SendChannel] for events going to other sites.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  open fun keepGoing(
      incoming: ReceiveChannel<*>,
      outgoing: SendChannel<*>,
  ): Boolean {
    return !(incoming.isClosedForReceive && outgoing.isClosedForSend)
  }

  /** Computes the next step. */
  abstract suspend fun step(): IncomingStep<T>
}

/**
 * Indicates that a step with the given name should not be reachable.
 *
 * @param name the name of the unreachable step.
 */
private fun notReachable(name: String? = null): Nothing {
  error("State ${name?.plus(" ")}should not be reachable")
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

  override suspend fun step(): IncomingStep<T> = { log ->
    select {
      val pending = pendingSites.lastOrNull()
      if (pending != null) {
        onSend(Inc.Advertisement(pending)) {
          advertisedSites.add(pending)
          pendingSites.removeLast()
          this@IncomingNew // mutable state updated.
        }
      } else {
        onSend(Inc.Ready) {
          withLock {
            IncomingSending<T>(
                    advertisedSites = advertisedSites,
                    pendingEvents = emptyList(),
                    pendingSites = emptyList(),
                    receivedAcks = emptyMap(),
                    receivedCredits = emptyMap(),
                )
                .update(log)
          }
        }
      }
      onReceiveOrClosed { v ->
        when (v.valueOrNull) {
          Out.Done, null -> IncomingCancelling()
          else -> this@IncomingNew // NoOp. TODO : Handle things differently ?
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
      log: EventLog<T>,
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

  override suspend fun step(): IncomingStep<T> = { log ->
    select {
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
          IncomingSending(
              advertisedSites = advertisedSites,
              pendingEvents = newEvents,
              pendingSites = pendingSites,
              receivedAcks = newAcks,
              receivedCredits = newCredits,
          )
        }
      }
      val firstSite = pendingSites.firstOrNull()
      if (firstSite != null) {
        onSend(Inc.Advertisement(firstSite)) {
          withLock {
            IncomingSending(
                    advertisedSites = advertisedSites.plus(firstSite),
                    pendingEvents = pendingEvents,
                    pendingSites = pendingSites.drop(1),
                    receivedAcks = receivedAcks,
                    receivedCredits = receivedCredits,
                )
                .update(log)
          }
        }
      }
      onReceiveOrClosed { v ->
        when (val msg = v.valueOrNull) {
          is Out.Request -> {
            // Ack based on request, and set credits for the given site.
            val ackForSite =
                maxOf(
                    receivedAcks[msg.site] ?: msg.nextForSite,
                    msg.nextForSite,
                )
            val newAcks = receivedAcks + (msg.site to ackForSite)
            val newCredits = receivedCredits + (msg.site to msg.count)
            IncomingSending(
                advertisedSites = advertisedSites,
                pendingEvents = pendingEvents,
                pendingSites = pendingSites,
                receivedAcks = newAcks,
                receivedCredits = newCredits,
            )
          }
          is Out.Done, null -> IncomingCancelling()
        }
      }
      onInsert { withLock { update(log) } }
    }
  }
}

// 1. We can send a Done message, and move to Completed.
private class IncomingCancelling<T> : IncomingState<T>() {

  override suspend fun step(): IncomingStep<T> = {
    select { onSend(Inc.Done) { IncomingCompleted() } }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is IncomingCancelling<*>) return false

    return true
  }

  override fun hashCode(): Int {
    return 31
  }
}

// 1. We receive a Done message, and move to Completed.
private class IncomingCompleting<T> : IncomingState<T>() {
  override suspend fun step(): IncomingStep<T> = {
    select {
      onReceiveOrClosed { v ->
        when (v.valueOrNull) {
          is Out.Done, null -> IncomingCompleted()
          is Out.Request -> this@IncomingCompleting // Draining.
        }
      }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is IncomingCompleting<*>) return false

    return true
  }

  override fun hashCode(): Int {
    return 31
  }
}

private class IncomingCompleted<T> : IncomingState<T>() {

  override fun keepGoing(
      incoming: ReceiveChannel<*>,
      outgoing: SendChannel<*>,
  ): Boolean = false

  override suspend fun step(): IncomingStep<T> = notReachable("Completed")

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is IncomingCompleted<*>) return false

    return true
  }

  override fun hashCode(): Int {
    return 31
  }
}
