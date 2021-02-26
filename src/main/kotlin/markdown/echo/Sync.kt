@file:Suppress("DuplicatedCode")

package markdown.echo

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Syncs the provided [Echo] until they are all done communicating. The [sync] operator creates a
 * chain of [Echo], and for each pair of the chain, some [Exchange] that are then used for
 * communication until all the data is eventually synced.
 *
 * Because a chain is created, if an [Echo] stops exchanging messages in the middle, the extremities
 * of the chain will not be able to communicate anymore. The degenerate case of the chain is a pair
 * of [Echo], which will simply exchange until they're done syncing.
 *
 * @param I the type of the incoming messages.
 * @param O the type of the outgoing messages.
 */
suspend fun <I, O> sync(
    first: Echo<I, O>,
    vararg others: Echo<I, O>,
) = coroutineScope {
    for (index in others.indices) {
        val left = if (index == 0) first else others[index - 1]
        val right = others[index]

        val lIncoming = left.incoming()
        val lOutgoing = left.outgoing()
        val rIncoming = right.incoming()
        val rOutgoing = right.outgoing()

        val lToROutgoing = Channel<O>()
        val lToRIncoming = Channel<I>()
        val rToLOutgoing = Channel<O>()
        val rToLIncoming = Channel<I>()

        launch {
            lIncoming
                .talk(rToLOutgoing.consumeAsFlow())
                .onEach { lToRIncoming.send(it) }
                .onCompletion { lToRIncoming.close() }
                .collect()
        }

        launch {
            lOutgoing
                .talk(rToLIncoming.consumeAsFlow())
                .onEach { lToROutgoing.send(it) }
                .onCompletion { lToROutgoing.close() }
                .collect()
        }

        launch {
            rIncoming
                .talk(lToROutgoing.consumeAsFlow())
                .onEach { rToLIncoming.send(it) }
                .onCompletion { rToLIncoming.close() }
                .collect()
        }

        launch {
            rOutgoing
                .talk(lToRIncoming.consumeAsFlow())
                .onEach { rToLOutgoing.send(it) }
                .onCompletion { rToLOutgoing.close() }
                .collect()
        }
    }
}
