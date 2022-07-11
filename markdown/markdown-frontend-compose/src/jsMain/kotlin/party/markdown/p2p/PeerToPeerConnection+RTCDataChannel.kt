package party.markdown.p2p

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

/**
 * Pipes the elements of the receiver [ReceiveChannel] to the given [SendChannel].
 *
 * @param T the type of the piped elements.
 * @receiver the [ReceiveChannel] which is receives some elements to be piped.
 * @param channel the [SendChannel] to which the elements are sent.
 */
suspend fun <T> ReceiveChannel<T>.pipeTo(
    channel: SendChannel<T>,
): Unit = consumeAsFlow().onEach(channel::send).onCompletion { channel.close() }.collect()
