@file:Suppress("SameParameterValue")

package io.github.alexandrepiveteau.echo.protocol.fsm

import io.github.alexandrepiveteau.echo.EchoEventLogPreview
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.ImmutableEventLog
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.selects.select

/**
 * A sealed class representing the different states that the finite state machine may be in. Each
 * step may decide to continue or stop processing with the [keepGoing] method, and defines state
 * transitions as part of the [step].
 *
 * @param T the type of the events.
 */
// TODO : Update the documentation.
internal sealed class OutgoingState<T, C> : State<Inc<T>, Out<T>, T, C, OutgoingState<T, C>> {

  companion object {

    /** Creates a new [OutgoingState] that's the beginning of the FSM. */
    operator fun <T, C> invoke(): OutgoingState<T, C> = OutgoingAdvertising(persistentListOf())
  }
}

/**
 * Indicates that a step with the given name should not be reachable.
 *
 * @param name the name of the unreachable step.
 */
private fun notReachable(name: String? = null): Throwable {
  return IllegalStateException("State ${name?.plus(" ")}should not be reachable")
}

// FINITE STATE MACHINE

private data class OutgoingAdvertising<T, C>(
    private val available: PersistentList<SiteIdentifier>,
) : OutgoingState<T, C>() {

  override suspend fun OutgoingStepScope<T, C>.step(
      log: ImmutableEventLog<T, C>,
  ): Effect<OutgoingState<T, C>> =
      when (val msg = receiveCatching().getOrNull()) {
        is Inc.Advertisement -> {
          Effect.Move(copy(available = available.add(msg.site)))
        }
        is Inc.Ready -> {
          Effect.Move(
              OutgoingListening(
                  pendingAcks = available,
                  pendingRequested = persistentListOf(),
                  requested = persistentListOf(),
              ))
        }
        null -> Effect.Terminate
        is Inc.Event -> Effect.MoveToError(notReachable())
      }
}

// TODO : Add more sophisticated precondition checks in protocol.
@OptIn(EchoEventLogPreview::class)
private data class OutgoingListening<T, C>(
    private val pendingAcks: PersistentList<SiteIdentifier>,
    private val pendingRequested: PersistentList<SiteIdentifier>,
    private val requested: PersistentList<SiteIdentifier>,
) : OutgoingState<T, C>() {

  private fun nextAcknowledgeOrNull(
      log: ImmutableEventLog<T, C>,
  ): Out.Acknowledge? {
    val acknowledge = pendingAcks.lastOrNull() ?: return null
    val expected = log.expected(acknowledge)
    return Out.Acknowledge(acknowledge, expected)
  }

  private fun handleAcknowledgeSent(msg: Out.Acknowledge): Effect<OutgoingState<T, C>> {
    return Effect.Move(
        copy(
            pendingAcks = pendingAcks.remove(msg.site),
            pendingRequested = pendingRequested.add(msg.site),
        ))
  }

  private fun nextRequestOrNull(): Out.Request? {
    val request = pendingRequested.firstOrNull() ?: return null
    return Out.Request(site = request, count = UInt.MAX_VALUE)
  }

  private fun handleRequestSent(msg: Out.Request): Effect<OutgoingState<T, C>> {
    return Effect.Move(
        copy(
            pendingRequested = pendingRequested.remove(msg.site),
            requested = requested.add(msg.site),
        ))
  }

  private fun handleAdvertisementReceived(msg: Inc.Advertisement): Effect<OutgoingState<T, C>> {
    return Effect.Move(copy(pendingAcks = pendingAcks.add(msg.site)))
  }

  private fun OutgoingStepScope<T, C>.handleEventReceived(
      msg: Inc.Event<T>,
  ): Effect<OutgoingState<T, C>> {
    set(msg.seqno, msg.site, msg.body)
    return Effect.Move(this@OutgoingListening)
  }

  override suspend fun OutgoingStepScope<T, C>.step(
      log: ImmutableEventLog<T, C>
  ): Effect<OutgoingState<T, C>> {

    return select {
      val acknowledge = nextAcknowledgeOrNull(log)
      if (acknowledge != null) {
        onSend(acknowledge) { handleAcknowledgeSent(acknowledge) }
      }

      val request = nextRequestOrNull()
      if (request != null) {
        onSend(request) { handleRequestSent(request) }
      }

      onReceiveCatching { v ->
        when (val msg = v.getOrNull()) {
          is Inc.Advertisement -> handleAdvertisementReceived(msg)
          is Inc.Event -> handleEventReceived(msg)
          is Inc.Ready -> Effect.MoveToError(notReachable())
          null -> Effect.Terminate
        }
      }
    }
  }
}
