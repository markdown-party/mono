@file:OptIn(InternalCoroutinesApi::class)
@file:Suppress("SameParameterValue")

package io.github.alexandrepiveteau.echo.protocol.fsm

import io.github.alexandrepiveteau.echo.EchoEventLogPreview
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.ImmutableEventLog
import io.github.alexandrepiveteau.echo.protocol.Message.V1.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.V1.Outgoing as Out
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.selects.select

/**
 * A sealed class representing the different states that the finite state machine may be in. Each
 * step may decide to continue or stop processing with the [keepGoing] method, and defines state
 * transitions as part of the [step].
 *
 * @param T the type of the events.
 */
// TODO : Update the documentation.
internal sealed class OutgoingState<T> : State<Inc<T>, Out<T>, T, OutgoingState<T>> {

  companion object {

    /** Creates a new [OutgoingState] that's the beginning of the FSM. */
    operator fun <T> invoke(): OutgoingState<T> = OutgoingAdvertising(mutableListOf())
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

private data class OutgoingAdvertising<T>(
    private val available: MutableList<SiteIdentifier>,
) : OutgoingState<T>() {

  @OptIn(InternalCoroutinesApi::class)
  override suspend fun OutgoingStepScope<T>.step(log: ImmutableEventLog<T>) =
      select<Effect<OutgoingState<T>>> {
        onReceiveOrClosed { v ->
          when (val msg = v.valueOrNull) {
            is Inc.Advertisement -> {
              available += msg.site
              Effect.Move(this@OutgoingAdvertising) // mutable state update.
            }
            is Inc.Ready -> {
              Effect.Move(
                  OutgoingListening(
                      pendingRequests = available,
                      requested = mutableListOf(),
                  ))
            }
            is Inc.Done, null -> Effect.Move(OutgoingCancelling())
            is Inc.Event -> Effect.MoveToError(notReachable())
          }
        }
      }
}

@OptIn(EchoEventLogPreview::class)
private data class OutgoingListening<T>(
    private val pendingRequests: MutableList<SiteIdentifier>,
    private val requested: MutableList<SiteIdentifier>,
) : OutgoingState<T>() {

  override suspend fun OutgoingStepScope<T>.step(
      log: ImmutableEventLog<T>
  ): Effect<OutgoingState<T>> {
    val request = pendingRequests.lastOrNull()
    val expected = request?.let(log::expected) ?: SequenceNumber.Zero
    val max = log.expected

    return select {
      if (request != null) {
        onSend(
            Out.Request(
                nextForAll = max,
                nextForSite = expected,
                site = request,
                count = Long.MAX_VALUE,
            )) {
          pendingRequests.removeLast()
          requested.add(request)
          Effect.Move(this@OutgoingListening) // mutable state update.
        }
      }

      onReceiveOrClosed { v ->
        when (val msg = v.valueOrNull) {
          is Inc.Done, null -> Effect.Move(OutgoingCancelling())
          is Inc.Advertisement -> {
            pendingRequests.add(msg.site)
            Effect.Move(this@OutgoingListening) // mutable state update.
          }
          is Inc.Event -> {
            set(msg.seqno, msg.site, msg.body)
            Effect.Move(this@OutgoingListening)
          }
          is Inc.Ready -> Effect.MoveToError(notReachable())
        }
      }
    }
  }
}

// 1. We can send a Done message, and move to Completed.
private class OutgoingCancelling<T> : OutgoingState<T>() {

  override suspend fun OutgoingStepScope<T>.step(
      log: ImmutableEventLog<T>,
  ) = select<Effect<OutgoingState<T>>> { onSend(Out.Done) { Effect.Terminate } }
}
