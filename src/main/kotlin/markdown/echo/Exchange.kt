@file:OptIn(
    ExperimentalCoroutinesApi::class,
    ExperimentalTypeInference::class,
)

package markdown.echo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import kotlin.experimental.ExperimentalTypeInference

/**
 * An [Exchange] allows for asymmetric communication, with a request-reply paradigm. The
 * [Exchange] issues some outgoing messages, and receives some incoming messages. The other party
 * receives some outgoing messages, and sends some incoming messages.
 *
 * @param [I] the type of the incoming messages.
 * @param [O] the type of the outgoing messages.
 */
fun interface Exchange<in I, out O> {

    /**
     * Starts an asymmetric communication.
     */
    fun talk(incoming: Flow<I>): Flow<O>
}

// BUILDER DSL SCOPES

typealias ExchangeFlowBuilder<I, O> = suspend FlowCollector<O>.(Flow<I>) -> Unit
typealias ExchangeChannelBuilder<I, O> = suspend ProducerScope<O>.(ReceiveChannel<I>) -> Unit

/**
 * Creates a _cold_ exchange from the given suspending [block]. The exchange being _cold_ means
 * that the [block] is called every time a terminal operator is applied to the resulting exchange.
 *
 * On a single [Echo], multiple [Exchange] might be open simultaneously. Therefore, exchanges should
 * make sure that concurrency is handled properly.
 *
 * @param block the initialization block for this [Exchange].
 *
 * @param I the type of the incoming messages.
 * @param O the type of the outgoing messages.
 */
fun <I, O> exchange(
    @BuilderInference block: ExchangeFlowBuilder<I, O>,
): Exchange<I, O> = Exchange { incoming ->
    flow { block.invoke(this, incoming) }
}

/**
 * Creates a _cold_ exchange from elements that are send to a send channel through a
 * [ProducerScope]. The incoming messages are produced through a [ReceiveChannel], passed through
 * the [block].
 *
 * On a single [Echo], multiple [Exchange] might be open simultaneously. Therefore, exchanges should
 * make sure that concurrency is handled properly.
 *
 * @param block the initialization block for this [Exchange].
 *
 * @param I the type of the incoming messages.
 * @param O the type of the outgoing messages.
 */
@ExperimentalCoroutinesApi
@OptIn(FlowPreview::class)
fun <I, O> channelExchange(
    @BuilderInference block: ExchangeChannelBuilder<I, O>,
): Exchange<I, O> = Exchange { incoming ->
    channelFlow { block.invoke(this, incoming.produceIn(this)) }
}
