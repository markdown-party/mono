package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.causal.EventIdentifier

internal data class EventValueEntry<out T>(
    override val identifier: EventIdentifier,
    override val body: T,
) : EventLog.Entry<T>
