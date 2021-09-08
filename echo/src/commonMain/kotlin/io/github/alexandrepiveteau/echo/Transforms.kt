package io.github.alexandrepiveteau.echo

import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.events.EventScope
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn

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
    object : MutableSite<T, M>, Exchange<Inc, Out> by FlowOnExchange(context, this) {

      override val identifier: SiteIdentifier
        get() = this@flowOn.identifier

      override val value: StateFlow<M>
        get() = this@flowOn.value

      override suspend fun <R> event(
          block: suspend EventScope<T>.(M) -> R,
      ) = this@flowOn.event(block)
    }
