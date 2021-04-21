package io.github.alexandrepiveteau.echo

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn

// BUFFERING

/**
 * Transforms an [Link] by buffering its contents. This buffers the underlying flows in both
 * directions.
 *
 * @param capacity the capacity of the buffer.
 *
 * @see [buffer] the buffer operator on the underlying [kotlinx.coroutines.flow.Flow].
 */
fun <I, O> Link<I, O>.buffer(
    capacity: Int = Channel.BUFFERED,
): Link<I, O> = Link { this.talk(it.buffer(capacity)).buffer(capacity) }

// An implementation of a Buffered Echo.
private class BufferedExchange<I, O>(
    private val capacity: Int,
    private val backing: Exchange<I, O>,
) : Exchange<I, O> {
  override fun outgoing() = backing.outgoing().buffer(capacity)
  override fun incoming() = backing.incoming().buffer(capacity)
}

/**
 * Transforms an [Link] by buffering its contents. This buffers the underlying flows in both
 * directions.
 *
 * @param capacity the capacity of the buffer.
 *
 * @see [buffer] the buffer operator on the underlying [kotlinx.coroutines.flow.Flow].
 */
fun <I, O> Exchange<I, O>.buffer(
    capacity: Int = Channel.BUFFERED,
): Exchange<I, O> = BufferedExchange(capacity, this)

// FLOW ON

/**
 * Transforms a [Link] by making it flow on a specific dispatcher. The same [CoroutineContext] will
 * be used in both directions.
 *
 * @param context the [CoroutineContext] to use for the flow.
 */
fun <I, O> Link<I, O>.flowOn(
    context: CoroutineContext,
): Link<I, O> = Link { this.talk(it.flowOn(context)).flowOn(context) }

// An implementation of a FlowOn Exchange.
private class FlowOnExchange<I, O>(
    private val context: CoroutineContext,
    private val backing: Exchange<I, O>,
) : Exchange<I, O> {
  override fun outgoing() = backing.outgoing().flowOn(context)
  override fun incoming() = backing.incoming().flowOn(context)
}

/**
 * Transforms an [Exchange] by making it flow on a specific dispatcher. The same [CoroutineContext]
 * will be used in both directions for both [Link]s.
 *
 * @param context the [CoroutineContext] to use for the flow.
 */
fun <I, O> Exchange<I, O>.flowOn(
    context: CoroutineContext,
): Exchange<I, O> = FlowOnExchange(context, this)
