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

/** A typealias that describes a step in the FSM. */
internal typealias Step<I, O, T, S> = suspend StepScope<I, O>.(MutableEventLog<T>) -> S

/** A step for [OutgoingState]. */
internal typealias OutgoingStep<T> = Step<Inc<T>, Out<T>, T, OutgoingState<T>>

/** A step for [IncomingState]. */
internal typealias IncomingStep<T> = Step<Out<T>, Inc<T>, T, IncomingState<T>>
