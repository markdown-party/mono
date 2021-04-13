package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.causal.EventIdentifier

/**
 * Creates an empty [ImmutableEventLog].
 *
 * @param T the type of the events in the log.
 */
fun <T> immutableEventLogOf(): ImmutableEventLog<T> = EmptyEventLog

/**
 * Creates a new instance of [ImmutableEventLog],
 *
 * @param events the pairs of event identifiers and event bodies to include in the log.
 *
 * @param T the type of events in the log.
 */
fun <T> immutableEventLogOf(
    vararg events: Pair<EventIdentifier, T>,
): ImmutableEventLog<T> = persistentEventLogOf(*events)

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
