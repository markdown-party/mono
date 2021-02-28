package markdown.echo

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map

/**
 * Transforms an [Exchange] with a bi-directional function, such that the inner messages are all
 * coded differently. The [coding] combinator does not allow encoding and decoding failures;
 * therefore, such failures should be thrown as exceptions and managed by the [Exchange]
 * implementations or their callers directly.
 *
 * @param incoming transforms the incoming messages to the existing message type.
 * @param outgoing transforms the outgoing messages to the new message type.
 */
fun <I1, O1, I2, O2> Exchange<I1, O1>.coding(
    incoming: suspend (I2) -> I1,
    outgoing: suspend (O1) -> O2,
): Exchange<I2, O2> = Exchange {
    this.talk(it.map(incoming)).map(outgoing)
}

/**
 * Transforms an [Exchange] by buffering its contents. This buffers the underlying flows in both
 * directions.
 *
 * @param capacity the capacity of the buffer.
 *
 * @see [buffer] the buffer operator on the underlying [kotlinx.coroutines.flow.Flow].
 */
fun <I, O> Exchange<I, O>.buffer(
    capacity: Int = Channel.BUFFERED,
): Exchange<I, O> = Exchange {
    this.talk(it.buffer(capacity)).buffer(capacity)
}

// An implementation of a Buffered Echo.
private class BufferedEcho<I, O>(
    private val capacity: Int,
    private val backing: Echo<I, O>,
) : Echo<I, O> {
    override fun outgoing() = backing.outgoing().buffer(capacity)
    override fun incoming() = backing.incoming().buffer(capacity)
}

/**
 * Transforms an [Exchange] by buffering its contents. This buffers the underlying flows in both
 * directions.
 *
 * @param capacity the capacity of the buffer.
 *
 * @see [buffer] the buffer operator on the underlying [kotlinx.coroutines.flow.Flow].
 */
fun <I, O> Echo<I, O>.buffer(
    capacity: Int = Channel.BUFFERED,
): Echo<I, O> = BufferedEcho(capacity, this)
