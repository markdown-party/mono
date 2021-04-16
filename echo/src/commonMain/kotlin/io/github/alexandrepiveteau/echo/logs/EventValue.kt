package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.causal.EventIdentifier

internal data class EventValueEntry<out T, out C>(
    override val identifier: EventIdentifier,
    override val body: T,
    override val change: Change<C>,
) : EventLog.Entry<T, C>
