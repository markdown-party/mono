package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.causal.EventIdentifier

/**
 * Creates a new instance of [PersistentEventLog].
 *
 * @param events the pairs of event identifiers and event bodies to include in the log.
 *
 * @param T the type of events in the log.
 */
fun <T> persistentEventLogOf(
    vararg events: Pair<EventIdentifier, T>,
): PersistentEventLog<T> = PersistentMapEventLog(*events)
