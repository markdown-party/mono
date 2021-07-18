package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier

/** The actual implementation of [AbstractMutableHistory] used in the builders. */
private class ActualMutableHistory<T>(
    initial: T,
    projection: MutableProjection<T>,
) : AbstractMutableHistory<T>(initial, projection)

/** The actual implementation of [AbstractMutableEventLog] used in the builders. */
private class ActualMutableEventLog : AbstractMutableEventLog()

/**
 * Creates a new [MutableHistory], with an aggregate with an [initial] value and a [projection] for
 * incremental changes.
 *
 * @param initial the initial aggregate value.
 * @param projection the [MutableProjection] value.
 *
 * @param T the type of the aggregate.
 */
fun <T> mutableHistoryOf(
    initial: T,
    projection: MutableProjection<T>,
): MutableHistory<T> = ActualMutableHistory(initial, projection)

/**
 * Creates a new [MutableHistory], with an aggregate with an [initial] value and a [projection] for
 * incremental changes.
 *
 * @param initial the initial aggregate value.
 * @param projection the [MutableProjection] value.
 * @param events some events to pre-populate the history.
 *
 * @param T the type of the aggregate.
 */
fun <T> mutableHistoryOf(
    initial: T,
    projection: MutableProjection<T>,
    vararg events: Pair<EventIdentifier, ByteArray>,
): MutableHistory<T> =
    ActualMutableHistory(initial, projection).apply {
      for ((id, body) in events) {
        insert(id.seqno, id.site, body)
      }
    }

/** Creates a new [MutableEventLog], with no aggregate. */
fun mutableEventLogOf(): MutableEventLog = mutableEventLogOf(*emptyArray())

/**
 * Creates a new [MutableEventLog], with no aggregate.
 *
 * @param events some events to pre-populate the history.
 */
fun mutableEventLogOf(
    vararg events: Pair<EventIdentifier, ByteArray>,
): MutableEventLog =
    ActualMutableEventLog().apply {
      for ((id, body) in events) {
        insert(id.seqno, id.site, body)
      }
    }
