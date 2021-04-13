package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.causal.EventIdentifier

/**
 * An alternative to [IndexedValue], identified by a unique [EventIdentifier].
 *
 * @param T the type of the event body.
 */
data class EventValue<out T>(
    val identifier: EventIdentifier,
    val value: T,
)
