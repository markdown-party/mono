package io.github.alexandrepiveteau.echo

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

// CODING

/**
 * Transforms an [Link] with a bi-directional function, such that the inner messages are all coded
 * differently. The [coding] combinator does not allow encoding and decoding failures; therefore,
 * such failures should be thrown as exceptions and managed by the [Link] implementations or their
 * callers directly.
 *
 * @param incoming transforms the incoming messages to the existing message type.
 * @param outgoing transforms the outgoing messages to the new message type.
 */
fun <I1, O1, I2, O2> Link<I1, O1>.coding(
    incoming: (I2) -> I1,
    outgoing: (O1) -> O2,
): Link<I2, O2> = Link { this.talk(it.map { e -> incoming(e) }).map { e -> outgoing(e) } }

/**
 * A simple bi-directional encoding and decoding mechanism.
 *
 * @param I the type of the unencoded content.
 * @param O the type of the encoded content.
 */
interface Coder<I, O> {
  fun encode(it: I): O
  fun decode(it: O): I
}

/** Reverses a [Coder] implementation. */
fun <I, O> Coder<I, O>.reversed(): Coder<O, I> =
    object : Coder<O, I> {
      override fun encode(it: O) = this@reversed.decode(it)
      override fun decode(it: I) = this@reversed.encode(it)
    }

/**
 * Transforms an [Exchange] with a pair of bi-directional functions, such that the incoming messages
 * are transformed to a certain type, and the outgoing messages are transformed to another type. The
 * [coding] combinator does not allow encoding and decoding failures; therefore, such failures
 * should be thrown as exceptions and managed by the [Exchange] callers directly.
 *
 * @param incoming a [Coder] acting as a bijection between [I1] and [I2].
 * @param outgoing a [Coder] acting as a bijection between [O1] and [O2].
 */
fun <I1, O1, I2, O2> Exchange<I1, O1>.coding(
    incoming: Coder<I1, I2>,
    outgoing: Coder<O1, O2>,
): Exchange<I2, O2> =
    object : Exchange<I2, O2> {
      override fun outgoing(): Link<I2, O2> =
          this@coding.outgoing()
              .coding(
                  incoming = incoming::decode,
                  outgoing = outgoing::encode,
              )
      override fun incoming(): Link<O2, I2> =
          this@coding.incoming()
              .coding(
                  incoming = outgoing::decode,
                  outgoing = incoming::encode,
              )
    }

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
