package markdown.echo

/**
 * An interface defining an asymmetrical replication site, biased towards sending data.
 *
 * @param I the type of the domain-specific incoming events for this [SendEcho].
 * @param O the type of the domain-specific outgoing events for this [SendEcho].
 */
fun interface SendEcho<I, O> {
    fun outgoing(): Exchange<I, O>
}

/**
 * An interface defining an asymmetrical replication site, biased towards receiving data.
 *
 * @param I the type of the domain-specific incoming events for this [ReceiveEcho].
 * @param O the type of the domain-specific outgoing events for this [ReceiveEcho].
 */
fun interface ReceiveEcho<I, O> {
    fun incoming(): Exchange<I, O>
}

/**
 * An interface defining an [Echo], which is able to generate some exchanges that are used for
 * bidirectional communication and exchange of data.
 *
 * @param I the type of the domain-specific incoming events for this [Echo].
 * @param O the type of the domain-specific outgoing events for this [Echo].
 */
interface Echo<I, O> : SendEcho<I, O>, ReceiveEcho<I, O> {

    // Useful for creating static functions.
    companion object
}
