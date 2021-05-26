@file:Suppress("SameParameterValue")

package io.github.alexandrepiveteau.echo.protocol.fsm

import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SequenceNumber.Companion.Zero
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.ImmutableEventLog
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import io.github.alexandrepiveteau.echo.protocol.fsm.Effect.Move
import io.github.alexandrepiveteau.echo.protocol.fsm.Effect.Terminate
import io.github.alexandrepiveteau.echo.util.plusBoundOverflows
import kotlinx.collections.immutable.*
import kotlinx.coroutines.selects.select

/**
 * A sealed class representing the different states that the finite state machine may be in. Each
 * step may decide to continue or stop processing with the [keepGoing] method, and defines state
 * transitions as part of the [step].
 *
 * @param T the type of the events.
 */
// TODO : Update the documentation.
internal sealed class IncomingState<T, C> : State<Out<T>, Inc<T>, T, C, IncomingState<T, C>> {

  companion object {

    /** Creates a new [IncomingState] that's the beginning of the FSM. */
    operator fun <T, C> invoke(): IncomingState<T, C> =
        IncomingNew(alreadySent = persistentListOf())
  }
}

// FINITE STATE MACHINE

// 1. We have some pending advertisements to send before we can issue the Ready, so
//    we simply issue them and move them from our pending queue to the advertised
//    queue.
// 2. We have advertised all of our pending sites, so we can issue a Ready event and
//    move to the Sending state.
// 3. We receive a Done event, so we move to Cancelling.
// 4. We receive an unsupported message, so we fail.
private data class IncomingNew<T, C>(
    private val alreadySent: PersistentList<EventIdentifier>,
) : IncomingState<T, C>() {

  /** Returns the next [Inc.Advertisement] message to send, if there's any. */
  private fun nextAdvertisementOrNull(log: ImmutableEventLog<*, *>): Inc.Advertisement? {
    val site = log.sites.minus(alreadySent.map { it.site }).firstOrNull() ?: return null
    return Inc.Advertisement(site, log.expected(site))
  }

  /**
   * Handles an advertisement message being successfully sent. This will update the list of
   * advertisements sent before the [Inc.Ready] message, and remove the site from the remaining
   * sites to send before the [Inc.Ready] message.
   *
   * @param msg the [Inc.Advertisement] message that was sent.
   */
  private fun handleAdvertisementSent(
      msg: Inc.Advertisement,
  ): Effect<IncomingState<T, C>> {
    val sent = alreadySent.add(EventIdentifier(msg.nextSeqno, msg.site))
    return Move(copy(alreadySent = sent))
  }

  /**
   * Handles the [Inc.Ready] message being sent, indicating that we are now done with the handshake
   * and can send events (and receive acknowledgements).
   */
  private fun handleReadySent(): Effect<IncomingState<T, C>> {
    // TODO : Eventually use the whole EventIdentifier.
    return Move(IncomingSending(advertised = alreadySent.map { it.site }.toPersistentList()))
  }

  override suspend fun IncomingStepScope<T, C>.step(
      log: ImmutableEventLog<T, C>
  ): Effect<IncomingState<T, C>> {
    return select {

      // Priority is given to the reception of cancellation messages.
      onReceiveCatching { v ->
        when (v.getOrNull()) {
          null -> Terminate
          else -> Effect.MoveToError(IllegalStateException())
        }
      }

      // Try to send an advertisement, or Ready if we're done advertising sites.
      val pending = nextAdvertisementOrNull(log)
      if (pending != null) {
        // TODO : Investigate if this is an issue and if we need to better define sync semantics.
        // There is a small non-intuitive behavior here, if the log is updated via another site
        // before  the expected from another site is sent, and we may have some events with missing
        // dependencies (at the application level).
        //
        // On one hand, these missing dependencies should already be handled by the business logic,
        // but on the other hand, the semantics are not obvious to a user.
        onSend(pending) { handleAdvertisementSent(pending) }
      } else {
        onSend(Inc.Ready) { handleReadySent() }
      }
    }
  }
}

private data class IncomingSending<T, C>(
    private val advertised: PersistentList<SiteIdentifier>,
    private val nextSequenceNumberPerSite: PersistentMap<SiteIdentifier, SequenceNumber> =
        persistentMapOf(),
    private val receivedCredits: PersistentMap<SiteIdentifier, UInt> = persistentMapOf(),
) : IncomingState<T, C>() {

  /** Returns the next [Inc.Advertisement] message to send, if there's any. */
  fun nextAdvertisementOrNull(log: ImmutableEventLog<T, C>): Inc.Advertisement? {
    val missing = log.sites.asSequence().minus(advertised).firstOrNull() ?: return null
    val seqno = log.expected(missing)
    return Inc.Advertisement(site = missing, nextSeqno = seqno)
  }

  /**
   * Returns the [IncomingState] if the [Inc.Advertisement] for the given [msg] was successfully
   * processed.
   */
  fun handleAdvSent(msg: Inc.Advertisement): IncomingState<T, C> {
    return copy(advertised = advertised + msg.site)
  }

  /**
   * Returns the next [Inc.Event] message to send, if there's any.
   *
   * @param log the [ImmutableEventLog] that is in charge of generating the events.
   */
  @Suppress("DEPRECATION")
  fun nextEventOrNull(log: ImmutableEventLog<T, C>): Inc.Event<T>? {
    val event =
        receivedCredits
            .asSequence()
            .filter { (_, credits) -> credits > 0U }
            .filter { (site, _) -> site in advertised }
            .mapNotNull { (site, _) ->
              nextSequenceNumberPerSite[site]?.let { log.events(site, it).firstOrNull() }
            }
            .minByOrNull { it.identifier }
            ?: return null

    return Inc.Event(
        seqno = event.identifier.seqno,
        site = event.identifier.site,
        body = event.body,
    )
  }

  /**
   * Returns the [IncomingState] if the [Inc.Event] for the given [msg] was successfully processed.
   */
  fun handleEventSent(msg: Inc.Event<T>): IncomingState<T, C> {
    val remainingCredits = receivedCredits[msg.site]?.minus(1U) ?: 0U
    val nextSequenceNumber = maxOf(msg.seqno.inc(), Zero)
    return copy(
        nextSequenceNumberPerSite = nextSequenceNumberPerSite.put(msg.site, nextSequenceNumber),
        receivedCredits = receivedCredits.put(msg.site, remainingCredits),
    )
  }

  /** Returns the [IncomingState] if the [Out.Acknowledge] [msg] is received. */
  fun handleAcknowledgeReceived(msg: Out.Acknowledge): IncomingState<T, C> {
    return copy(
        nextSequenceNumberPerSite = nextSequenceNumberPerSite.put(msg.site, msg.nextSeqno),
        receivedCredits = receivedCredits.put(msg.site, 0U),
    )
  }

  /** Returns the [IncomingState] if the [Out.Request] [msg] is received. */
  fun handleRequestReceived(msg: Out.Request): IncomingState<T, C> {
    val currentCredits = receivedCredits[msg.site] ?: 0U
    val remainingCredits = currentCredits.plusBoundOverflows(msg.count)
    return copy(
        receivedCredits = receivedCredits.put(msg.site, remainingCredits),
    )
  }

  override suspend fun IncomingStepScope<T, C>.step(
      log: ImmutableEventLog<T, C>
  ): Effect<IncomingState<T, C>> {
    return select {
      onReceiveCatching { v ->
        when (val msg = v.getOrNull()) {
          is Out.Acknowledge -> Move(handleAcknowledgeReceived(msg))
          is Out.Request -> Move(handleRequestReceived(msg))
          null -> Terminate
        }
      }

      val event = nextEventOrNull(log)
      if (event != null) onSend(event) { Move(handleEventSent(event)) }

      val advertisement = nextAdvertisementOrNull(log)
      if (advertisement != null) onSend(advertisement) { Move(handleAdvSent(advertisement)) }

      // On new events, request another pass of the step.
      onInsert { Move(this@IncomingSending) }
    }
  }
}
