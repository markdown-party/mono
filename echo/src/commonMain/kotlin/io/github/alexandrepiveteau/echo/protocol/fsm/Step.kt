package io.github.alexandrepiveteau.echo.protocol.fsm

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.ImmutableEventLog
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.SelectClause1

/**
 * A scope that's available when creating the next FSM step. This provides access to the following
 * parts :
 *
 * - A [ReceiveChannel] for incoming messages.
 * - A [SendChannel] for outgoing messages.
 *
 * @param I the type of the incoming messages.
 * @param O the type of the outgoing messages.
 * @param C the type of the changes.
 */
internal interface StepScope<out I, in O, T, C> : ReceiveChannel<I>, SendChannel<O> {

  /** A [SelectClause1] that's made available when a new value is inserted in the log. */
  val onInsert: SelectClause1<ImmutableEventLog<T, C>>

  /**
   * Sets the [event] for a certain [seqno] and a given [site]. This will mutate the current site,
   * but won't affect already emitted [ImmutableEventLog] instances.
   */
  fun set(seqno: SequenceNumber, site: SiteIdentifier, event: T)
}

/** A specific version of [StepScope] that receives [Inc] messages and sends [Out] messages. */
internal typealias OutgoingStepScope<T, C> = StepScope<Inc<T>, Out<T>, T, C>

/** A specific version of [StepScope] that receives [Out] messages and sends [Inc] messages. */
internal typealias IncomingStepScope<T, C> = StepScope<Out<T>, Inc<T>, T, C>

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

/**
 * An interface defining a [State] from a final state machine. When the [step] function is invoked,
 * the [State] chooses what state to move in in a suspending fashion. It has access to the following
 * information :
 *
 * - Received messages of type [I].
 * - Sent messages of type [O].
 * - The current [ImmutableEventLog], with events of type [T].
 *
 * At the end of the [step] invocation, an [Effect] is issued, which determines what the next FSM
 * state will be.
 *
 * @param I the type of the received messages.
 * @param O the type of the sent messages.
 * @param T the type of the body of the events.
 * @param C the type of the changes of the events.
 * @param S the type of the [Effect] states.
 */
// TODO : Make this a fun interface when b/KT-40165 is fixed.
/* fun */ internal interface State<I, O, T, C, S : State<I, O, T, C, S>> {

  /** Performs a suspending step of this FSM. */
  suspend fun StepScope<I, O, T, C>.step(
      log: ImmutableEventLog<T, C>,
  ): Effect<S>
}
