package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.*
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.EventLog
import io.github.alexandrepiveteau.echo.core.log.MutableEventLog
import io.github.alexandrepiveteau.echo.core.log.MutableHistory
import io.github.alexandrepiveteau.echo.events.EventScope
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import io.github.alexandrepiveteau.echo.sync.SyncStrategy
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.selects.SelectInstance
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer

internal open class ExchangeImpl(
    private val log: MutableEventLog,
    private val strategy: SyncStrategy,
) : Exchange<Inc, Out> {

  /** The [Mutex] that protects access to the [sentinel] variable. */
  internal val mutex = Mutex()

  /**
   * A sentinel [MutableStateFlow] that can be used to observe invalidations of the [log]. Whenever
   * a site gains mutual exclusion to the [MutableEventLog].
   */
  private val sentinel = MutableStateFlow(UInt.MIN_VALUE)

  /**
   * A function that will be called whenever some mutations were performed, and some computed values
   * or the event log should be updated.
   */
  open fun mutation() {
    sentinel.value = sentinel.value + 1U
  }

  /**
   * Runs an exchange in an [ExchangeBlock]. The implementation of the exchange may be a finite
   * state machine, or a simple suspending function that performs linear and consecutive steps.
   *
   * @param block the [ExchangeBlock] that will be run.
   *
   * @param I the type of the input messages.
   * @param O the type of the output messages.
   */
  private fun <I, O> exchange(
      block: ExchangeBlock<I, O>,
  ): Link<I, O> = channelLink { inc ->
    val channel = sentinel.produceIn(this)
    val scope = ExchangeScopeImpl(mutex, log, channel, inc, this, ::mutation)

    // Give the other threads a chance to run and generate some (termination ?) messages, and then
    // launch our exchange.
    yield()
    block(scope)

    // Clear the jobs registered in channelLink, and ensure proper termination of the exchange.
    inc.cancel()
    channel.cancel()
  }

  override fun outgoing() = exchange<Inc, Out> { with(strategy) { outgoing() } }
  override fun incoming() = exchange<Out, Inc> { with(strategy) { incoming() } }
}

internal open class SiteImpl<M, R>(
    private val history: MutableHistory<R>,
    strategy: SyncStrategy,
    private val transform: (R) -> M,
) : ExchangeImpl(history, strategy), Site<M> {

  /**
   * The current [history] value. Accesses to set the [current] value are protected through mutual
   * exclusion via the [Mutex] variable.
   */
  internal val current = MutableStateFlow(transform(history.current))

  override val value = current.asStateFlow()

  override fun mutation() {
    current.value = transform(history.current)
    super.mutation()
  }
}

internal open class MutableSiteImpl<T, M, R>(
    override val identifier: SiteIdentifier,
    private val serializer: KSerializer<T>,
    history: MutableHistory<R>,
    format: BinaryFormat,
    strategy: SyncStrategy,
    transform: (R) -> M,
) :
    SiteImpl<M, R>(
        history = history,
        strategy = strategy,
        transform = transform,
    ),
    MutableSite<T, M> {

  private val scope =
      object : EventScope<T> {
        override fun yield(event: T): EventIdentifier =
            history.append(
                    site = identifier,
                    event = format.encodeToByteArray(serializer, event),
                )
                .apply { mutation() }
      }

  override suspend fun event(
      block: suspend EventScope<T>.(M) -> Unit,
  ) = mutex.withLock { block(scope, value.value) }
}

/**
 * An implementation of [ExchangeScope] that provides interoperability with a [MutableSiteImpl]. It
 * takes care of implementing mutual exclusion when accessing the [EventLog].
 *
 * @param mutex the [Mutex] used to provide mutual exclusion.
 * @param log the [MutableEventLog] that might be accessed.
 * @param sentinel a [ReceiveChannel] emitting new values whenever the log is updated.
 * @param incoming a [ReceiveChannel] with incoming messages.
 * @param outgoing a [SendChannel] with outgoing messages.
 * @param mutation a lambda that should be called with mutual exclusion when the log is mutated.
 *
 * @param I the type of the incoming messages.
 * @param O the type of the outgoing messages.
 */
@OptIn(InternalCoroutinesApi::class)
private class ExchangeScopeImpl<I, O>(
    private val mutex: Mutex,
    private val log: MutableEventLog,
    private val sentinel: ReceiveChannel<*>,
    incoming: ReceiveChannel<I>,
    outgoing: SendChannel<O>,
    private val mutation: () -> Unit,
) : ExchangeScope<I, O>, ReceiveChannel<I> by incoming, SendChannel<O> by outgoing {

  override suspend fun <R> withEventLogLock(
      block: suspend EventLog.() -> R,
  ) = mutex.withLock { block(log) }

  override suspend fun <R> withMutableEventLogLock(
      block: suspend MutableEventLog.() -> R,
  ) = mutex.withLock { block(log).also { mutation() } }

  override val onEventLogLock =
      object : SelectClause1<MutableEventLog> {
        override fun <R> registerSelectClause1(
            select: SelectInstance<R>,
            block: suspend (MutableEventLog) -> R,
        ) = mutex.onLock.registerSelectClause2(select, null) { block(log).also { mutex.unlock() } }
      }

  override val onMutableEventLogLock =
      object : SelectClause1<MutableEventLog> {
        override fun <R> registerSelectClause1(
            select: SelectInstance<R>,
            block: suspend (MutableEventLog) -> R,
        ) =
            mutex.onLock.registerSelectClause2(select, null) {
              block(log).also {
                mutation()
                mutex.unlock()
              }
            }
      }

  override val onEventLogUpdate =
      object : SelectClause0 {
        override fun <R> registerSelectClause0(
            select: SelectInstance<R>,
            block: suspend () -> R,
        ) = sentinel.onReceive.registerSelectClause1(select) { block() }
      }
}
