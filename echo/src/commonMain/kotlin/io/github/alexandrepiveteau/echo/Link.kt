@file:OptIn(
    ExperimentalCoroutinesApi::class,
    ExperimentalTypeInference::class,
)

package io.github.alexandrepiveteau.echo

import kotlin.experimental.ExperimentalTypeInference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*

/**
 * An [Link] allows for asymmetric communication, with a request-reply paradigm. The [Link] issues
 * some outgoing messages, and receives some incoming messages. The other party receives some
 * outgoing messages, and sends some incoming messages.
 *
 * @param [I] the type of the incoming messages.
 * @param [O] the type of the outgoing messages.
 */
fun interface Link<in I, out O> {

  /** Starts an asymmetric communication. */
  fun talk(incoming: Flow<I>): Flow<O>
}

// BUILDERS

/**
 * Creates a _cold_ link from the given suspending [block]. The link being _cold_ means that the
 * [block] is called every time a terminal operator is applied to the resulting link.
 *
 * On a single [Exchange], multiple [Link] might be open simultaneously. Therefore, links should
 * make sure that concurrency is handled properly.
 *
 * @param block the initialization block for this [Link].
 *
 * @param I the type of the incoming messages.
 * @param O the type of the outgoing messages.
 */
fun <I, O> link(
    @BuilderInference block: suspend FlowCollector<O>.(Flow<I>) -> Unit,
): Link<I, O> = Link { incoming -> flow { block.invoke(this, incoming) } }

/**
 * Creates a _cold_ link from elements that are send to a send channel through a [ProducerScope].
 * The incoming messages are produced through a [ReceiveChannel], passed through the [block].
 *
 * On a single [Exchange], multiple [Link] might be open simultaneously. Therefore, links should
 * make sure that concurrency is handled properly.
 *
 * @param block the initialization block for this [Link].
 *
 * @param I the type of the incoming messages.
 * @param O the type of the outgoing messages.
 */
@ExperimentalCoroutinesApi
@OptIn(FlowPreview::class)
fun <I, O> channelLink(
    @BuilderInference block: suspend ProducerScope<O>.(ReceiveChannel<I>) -> Unit,
): Link<I, O> = Link { incoming -> channelFlow { block.invoke(this, incoming.produceIn(this)) } }
