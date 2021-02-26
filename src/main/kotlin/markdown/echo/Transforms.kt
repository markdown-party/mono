package markdown.echo

import kotlinx.coroutines.flow.map

/**
 * Transforms an [Exchange] with a bi-directional function, such that the inner messages are all
 * coded differently. The [coding] combinator does not allow encoding and decoding failures;
 * therefore, such failures should be thrown as exceptions and managed by the [Exchange]
 * implementations or their callers directly.
 *
 * @param incoming Transforms the incoming messages to the existing message type.
 * @param outgoing Transforms the outgoing messages to the new message type.
 */
fun <I1, O1, I2, O2> Exchange<I1, O1>.coding(
    incoming: suspend (I2) -> I1,
    outgoing: suspend (O1) -> O2,
): Exchange<I2, O2> = Exchange {
    this.talk(it.map(incoming)).map(outgoing)
}
