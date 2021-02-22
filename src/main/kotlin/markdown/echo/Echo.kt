package markdown.echo

/**
 * An interface defining an [Echo], which is able to generate some exchanges that are used for
 * bidirectional communication and exchange of data.
 *
 * @param T the type of the domain-specific events for this [Echo].
 */
interface Echo<T> {

    // Useful for creating static functions.
    companion object
}
