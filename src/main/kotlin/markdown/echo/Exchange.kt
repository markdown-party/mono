package markdown.echo

import kotlinx.coroutines.flow.Flow

/**
 * An [Exchange] allows for asymmetric communication, with a request-reply paradigm. The
 * [Exchange] issues some outgoing messages, and receives some incoming messages. The other party
 * receives some outgoing messages, and sends some incoming messages.
 *
 * @param [I] the type of the incoming messages.
 * @param [O] the type of the outgoing messages.
 */
fun interface Exchange<I, O> {

    /**
     * Starts an asymmetric communication.
     */
    fun talk(incoming: Flow<I>): Flow<O>
}
