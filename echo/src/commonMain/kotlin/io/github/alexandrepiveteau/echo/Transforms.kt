@file:JvmName("Exchanges")
@file:JvmMultifileClass

package io.github.alexandrepiveteau.echo

import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.events.EventScope
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

// BUFFERING

// An implementation of a Buffered Echo.
private class BufferedExchange<I, O>(
    private val capacity: Int,
    private val backing: Exchange<I, O>,
) : Exchange<I, O> {

  override fun receive(
      incoming: Flow<O>,
  ) = backing.receive(incoming.buffer(capacity)).buffer(capacity)

  override fun send(
      incoming: Flow<I>,
  ) = backing.send(incoming.buffer(capacity)).buffer(capacity)
}

/**
 * Transforms an [Exchange] by buffering its contents. This buffers the underlying flows in both
 * directions.
 *
 * @param capacity the capacity of the buffer.
 *
 * @see [buffer] the buffer operator on the underlying [Flow].
 */
fun <I, O> Exchange<I, O>.buffer(
    capacity: Int = Channel.BUFFERED,
): Exchange<I, O> = BufferedExchange(capacity, this)

// FLOW ON

// An implementation of a FlowOn Exchange.
private class FlowOnExchange<I, O>(
    private val context: CoroutineContext,
    private val backing: Exchange<I, O>,
) : Exchange<I, O> {

  override fun receive(
      incoming: Flow<O>,
  ) = backing.receive(incoming.flowOn(context)).flowOn(context)

  override fun send(
      incoming: Flow<I>,
  ) = backing.send(incoming.flowOn(context)).flowOn(context)
}

/**
 * Transforms an [Exchange] by making it flow on a specific dispatcher. The same [CoroutineContext]
 * will be used in both directions for the communicating [Flow]s.
 *
 * @param context the [CoroutineContext] to use for the flow.
 */
fun <I, O> Exchange<I, O>.flowOn(
    context: CoroutineContext,
): Exchange<I, O> = FlowOnExchange(context, this)

/**
 * Transforms a [MutableSite] by making it flow on a specific dispatcher. The same
 * [CoroutineContext] will be used in both directions for the communicating [Flow]s.
 *
 * @param context the [CoroutineContext] to use for the flow.
 */
fun <T, M> MutableSite<T, M>.flowOn(
    context: CoroutineContext,
): MutableSite<T, M> =
    object :
        MutableSite<T, M>,
        StateFlow<M> by this,
        Exchange<Inc, Out> by FlowOnExchange(context, this) {

      override val identifier: SiteIdentifier
        get() = this@flowOn.identifier

      override suspend fun <R> event(
          block: suspend EventScope<T>.(M) -> R,
      ) = this@flowOn.event(block)
    }

// MAP

/**
 * Transforms a [MutableSite] to make it return a different kind of model. This may be particularly
 * useful when creating abstractions backed by a [MutableSite] that should not expose some
 * implementation details.
 *
 * @param f the mapping function.
 *
 * @param T the type of the events.
 * @param M1 the type of the original model.
 * @param M2 the type of the transformed model.
 */
fun <T, M1, M2> MutableSite<T, M1>.map(
    f: (M1) -> M2,
): MutableSite<T, M2> = MappingMutableSite(f, this)

/**
 * An implementation of [MutableSite] which maps the model with a dedicated function.
 *
 * @param f the mapping function.
 * @param backing the underlying [MutableSite].
 */
private class MappingMutableSite<T, out M1, out M2>(
    private val f: (M1) -> M2,
    private val backing: MutableSite<T, M1>
) : MutableSite<T, M2>, StateFlow<M2> by (backing as StateFlow<M1>).map(f) {

  override fun receive(incoming: Flow<Out>) = backing.receive(incoming)
  override fun send(incoming: Flow<Inc>) = backing.send(incoming)

  override val identifier = backing.identifier

  override suspend fun <R> event(
      block: suspend EventScope<T>.(M2) -> R,
  ) = backing.event { m -> block(this, f(m)) }
}

/** Maps a [StateFlow] with the given function [f] */
private fun <F, T> StateFlow<F>.map(
    f: (F) -> T,
): StateFlow<T> = MappingStateFlow(this, f)

/**
 * An implementation of [StateFlow] that maps a [backing] [StateFlow] with the function [f].
 *
 * @param F the type of the items of the [backing] [StateFlow].
 * @param T the type of the items of this [StateFlow].
 */
private class MappingStateFlow<in F, out T>(
    private val backing: StateFlow<F>,
    private val f: (F) -> T,
) : StateFlow<T> {

  override suspend fun collect(
      collector: FlowCollector<T>,
  ) =
      backing.collect(
          object : FlowCollector<F> {
            override suspend fun emit(value: F) {
              collector.emit(f(value))
            }
          },
      )

  override val replayCache: List<T>
    get() = backing.replayCache.map(f)

  override val value: T
    get() = f(backing.value)
}
