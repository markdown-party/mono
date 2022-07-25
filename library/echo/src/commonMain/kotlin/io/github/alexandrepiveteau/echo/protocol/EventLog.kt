package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.core.log.EventLog
import io.github.alexandrepiveteau.echo.core.log.EventLog.OnLogUpdateListener
import io.github.alexandrepiveteau.echo.core.log.History
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/**
 * Returns a conflated [Flow] of update events which have been performed on the receiver.
 *
 * @receiver the [EventLog] for which the events are queried.
 * @return the [Flow] of updates.
 */
internal fun EventLog.asMutationsFlow() = asBufferedMutationsFlow().buffer(CONFLATED)

/**
 * Returns a conflated [Flow] of the currently aggregated value of the [History].
 *
 * @param T the type of the aggregate.
 * @receiver the [History] for which the current value is observed.¨
 * @return the [Flow] of current values of type [T].
 */
internal fun <T> History<T>.asCurrentFlow() = asBufferedCurrentFlow().buffer(CONFLATED)

private fun EventLog.asBufferedMutationsFlow(): Flow<Unit> = callbackFlow {
  val listener = OnLogUpdateListener { trySend(Unit) }
  registerLogUpdateListener(listener)
  awaitClose { unregisterLogUpdateListener(listener) }
}

private fun <T> History<T>.asBufferedCurrentFlow(): Flow<T> = callbackFlow {
  val listener = History.OnValueUpdateListener<T> { trySend(it) }
  registerValueUpdateListener(listener)
  awaitClose { unregisterValueUpdateListener(listener) }
}
