package io.github.alexandrepiveteau.echo.webrtc.server.groups

/** Maps the given [Outbox] using the provided function. */
internal fun <A, B> Outbox<A>.map(
    f: suspend (B) -> A,
) = Outbox<B> { element -> this@map.sendCatching(f(element)) }
