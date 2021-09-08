package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.core.log.EventLog
import io.github.alexandrepiveteau.echo.core.log.MutableEventLog
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.SelectClause1

/**
 * The scope in which an exchange is performed with another site. More specifically, it offers some
 * facilities to send messages via a [SendChannel], to receive messages via a [ReceiveChannel], and
 * to perform some updates on the log with mutual exclusion.
 *
 * @param I the type of the incoming messages.
 * @param O the type of the outgoing messages.
 */
interface ExchangeScope<out I, in O> : ReceiveChannel<I>, SendChannel<O> {

  /**
   * The [MutableEventLog] instance, which can be used to receive and write some binary events from
   * other sites. It should only be accessed after between a [lock] and an [unlock] call.
   */
  val log: MutableEventLog

  /** Locks access to the [MutableEventLog]. */
  suspend fun lock()

  /** Unlocks access to the [MutableEventLog]. */
  fun unlock()

  /** Atomically marks that the [MutableEventLog] has been mutated. */
  fun mutate()

  /** A select clause that is made available when the log is available for reading. */
  val onEventLogLock: SelectClause1<EventLog>

  /** A select clause that is made available when the log is available for reading and writing. */
  val onMutableEventLogLock: SelectClause1<MutableEventLog>

  /**
   * A [SelectClause0] that is made available when the event log is updated with some new content.
   * This is typically used to ensure that the state is notified with the latest log state.
   */
  val onEventLogUpdate: SelectClause0
}

/**
 * Executes the given [block] on the [EventLog] with global exclusion. Because the [EventLog] may be
 * accessed concurrently, you should not reference the [EventLog] outside of this block.
 *
 * @param R the return type of the update [block].
 */
suspend inline fun <R> ExchangeScope<*, *>.withEventLogLock(block: EventLog.() -> R): R {
  lock()
  try {
    return block(log)
  } finally {
    unlock()
  }
}

/**
 * Executes the given [block] on the [MutableEventLog] with global exclusion. When in the body of
 * this method, you may write to the [MutableEventLog]. Because the [MutableEventLog] may be
 * accessed concurrently, you should not reference the [MutableEventLog] outside of this block.
 *
 * @param R the return type of the update [block].
 */
suspend inline fun <R> ExchangeScope<*, *>.withMutableEventLogLock(
    block: MutableEventLog.() -> R,
): R {
  lock()
  try {
    return block(log).apply { mutate() }
  } finally {
    unlock()
  }
}

/** A block of code to execute with an [ExchangeScope] context. */
internal typealias ExchangeBlock<I, O> = suspend ExchangeScope<I, O>.() -> Unit
