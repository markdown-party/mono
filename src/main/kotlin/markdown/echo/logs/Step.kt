package markdown.echo.logs

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.sync.Mutex
import markdown.echo.Message.V1.Incoming as Inc
import markdown.echo.Message.V1.Outgoing as Out
import markdown.echo.causal.EventIdentifier

/**
 * A scope that's available when creating the next FSM step. This provides access to the following
 * parts :
 *
 * - A [ReceiveChannel] for incoming messages.
 * - A [SendChannel] for outgoing messages.
 * - A [Mutex] for safe concurrent accesses across sites.
 *
 * @param I the type of the incoming messages.
 * @param O the type of the outgoing messages.
 */
internal interface StepScope<I, O> : ReceiveChannel<I>, SendChannel<O>, Mutex {

  /** A [SelectClause1] that's made available when a new value is inserted in the log. */
  val onInsert: SelectClause1<EventIdentifier?>
}

internal typealias OutgoingStepScope<T> = StepScope<Inc<T>, Out<T>>

internal typealias IncomingStepScope<T> = StepScope<Out<T>, Inc<T>>

/**
 * An [Effect] is a sealed class that is used to indicate what the next step of a finite state
 * machine will be.
 *
 * @param T the type of the steps.
 */
internal sealed class Effect<out T> {

  /** Moves the state machine to the given [next] state. */
  data class Move<out T>(val next: T) : Effect<T>()

  /** Moves the state machine to an error state. */
  data class MoveToError(val problem: Throwable) : Effect<Nothing>()

  /** Terminates (with success) the state machine. */
  object Terminate : Effect<Nothing>()
}

internal interface State<I, O, T, S : State<I, O, T, S>> {
  suspend fun StepScope<I, O>.step(log: MutableEventLog<T>): Effect<S>
}
