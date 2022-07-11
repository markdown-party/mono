package party.markdown.backend.groups

/** Maps the given [Outbox] using the provided function. */
fun <A, B> Outbox<A>.map(
    f: suspend (B) -> A,
) = Outbox<B> { element -> this@map.sendCatching(f(element)) }
