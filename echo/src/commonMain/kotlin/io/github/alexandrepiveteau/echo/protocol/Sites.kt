package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.Site
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.EventLog
import io.github.alexandrepiveteau.echo.core.log.MutableEventLog
import io.github.alexandrepiveteau.echo.core.log.MutableHistory
import io.github.alexandrepiveteau.echo.events.EventScope
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import io.github.alexandrepiveteau.echo.sync.SyncStrategy
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.SelectClause0
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

  /** The [Mutex] that protects access to the [log] variable. */
  internal val mutex = Mutex()

  /** A [MutableSharedFlow] that is used to mark that a mutation has occurred in the log. */
  private val mutations = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = DROP_OLDEST)

  /**
   * A function that will be called whenever some mutations were performed, and some computed values
   * or the event log should be updated.
   */
  open fun mutation() {
    mutations.tryEmit(Unit)
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
      incoming: Flow<I>,
      block: ExchangeBlock<I, O>,
  ): Flow<O> = channelFlow {
    val inc = incoming.produceIn(this)
    val channel = mutations.produceIn(this)
    val scope = ExchangeScopeImpl(mutex, log, channel, inc, this, ::mutation)

    // Give the other threads a chance to run and generate some (termination ?) messages, and then
    // launch our exchange.
    yield()
    block(scope)

    // Clear the jobs registered in channelFlow, and ensure proper termination of the exchange.
    inc.cancel()
    channel.cancel()
  }

  override fun send(
      incoming: Flow<Inc>,
  ): Flow<Out> = exchange(incoming) { with(strategy) { outgoing() } }

  override fun receive(
      incoming: Flow<Out>,
  ): Flow<Inc> = exchange(incoming) { with(strategy) { incoming() } }
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
    private val history: MutableHistory<R>,
    private val format: BinaryFormat,
    strategy: SyncStrategy,
    transform: (R) -> M,
) :
    SiteImpl<M, R>(
        history = history,
        strategy = strategy,
        transform = transform,
    ),
    MutableSite<T, M>,
    EventScope<T> {

  override fun yield(event: T) =
      history.append(
          site = identifier,
          event = format.encodeToByteArray(serializer, event),
      )

  override suspend fun <R> event(
      block: suspend EventScope<T>.(M) -> R,
  ) = mutex.withLock { block(this, value.value).apply { mutation() } }
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
    override val log: MutableEventLog,
    private val sentinel: ReceiveChannel<*>,
    incoming: ReceiveChannel<I>,
    outgoing: SendChannel<O>,
    private val mutation: () -> Unit,
) : ExchangeScope<I, O>, ReceiveChannel<I> by incoming, SendChannel<O> by outgoing {

  override suspend fun lock() = mutex.lock()
  override fun unlock() = mutex.unlock()
  override fun mutate() = mutation()

  override val onEventLogUpdate =
      object : SelectClause0 {
        override fun <R> registerSelectClause0(
            select: SelectInstance<R>,
            block: suspend () -> R,
        ) = sentinel.onReceive.registerSelectClause1(select) { block() }
      }
}
