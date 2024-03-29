package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.log.buffer.AbstractGapBufferMutableEventLog
import io.github.alexandrepiveteau.echo.core.log.buffer.AbstractGapBufferMutableHistory
import io.github.alexandrepiveteau.echo.core.log.tree.AbstractAVLTreeMutableEventLog
import kotlinx.datetime.Clock

/** An actual implementation of [AbstractGapBufferMutableHistory] used in the builders. */
private class ActualMutableHistory<T>(
    initial: T,
    projection: MutableProjection<T>,
    clock: Clock,
) : AbstractGapBufferMutableHistory<T>(initial, projection, clock)

/** An actual implementation of [AbstractGapBufferMutableEventLog] used in the builders. */
private class LinearMutableEventLog(clock: Clock) : AbstractGapBufferMutableEventLog(clock)

/** An actual implementation of [AbstractAVLTreeMutableEventLog] used in the builders. */
private class TreeMutableEventLog(clock: Clock) : AbstractAVLTreeMutableEventLog(clock)

/**
 * Creates a new [MutableHistory], with an aggregate with an [initial] value and a [projection] for
 * incremental changes.
 *
 * @param initial the initial aggregate value.
 * @param projection the [MutableProjection] value.
 * @param clock the [Clock] used to generate new events.
 *
 * @param T the type of the aggregate.
 */
public fun <T> mutableHistoryOf(
    initial: T,
    projection: MutableProjection<T>,
    clock: Clock = Clock.System,
): MutableHistory<T> = ActualMutableHistory(initial, projection, clock)

/**
 * Creates a new [MutableHistory], with an aggregate with an [initial] value and a [projection] for
 * incremental changes.
 *
 * @param initial the initial aggregate value.
 * @param projection the [MutableProjection] value.
 * @param events some events to pre-populate the history.
 * @param clock the [Clock] used to generate new events.
 *
 * @param T the type of the aggregate.
 */
public fun <T> mutableHistoryOf(
    initial: T,
    projection: MutableProjection<T>,
    vararg events: Pair<EventIdentifier, ByteArray>,
    clock: Clock = Clock.System,
): MutableHistory<T> =
    ActualMutableHistory(initial, projection, clock).apply {
      for ((id, body) in events) {
        insert(id.seqno, id.site, body)
      }
    }

/**
 * Creates a new [MutableEventLog], with no aggregate.
 *
 * @param clock the [Clock] used to generate new events.
 */
public fun mutableEventLogOf(
    clock: Clock = Clock.System,
): MutableEventLog = LinearMutableEventLog(clock)

/**
 * Creates a new [MutableEventLog], with no aggregate.
 *
 * @param clock the [Clock] used to generate new events.
 */
public fun mutableTreeEventLogOf(
    clock: Clock = Clock.System,
): MutableEventLog = TreeMutableEventLog(clock)

/**
 * Creates a new [MutableEventLog], with no aggregate.
 *
 * @param events some events to pre-populate the history.
 * @param clock the [Clock] used to generate new events.
 */
public fun mutableEventLogOf(
    vararg events: Pair<EventIdentifier, ByteArray>,
    clock: Clock = Clock.System,
): MutableEventLog =
    LinearMutableEventLog(clock).apply {
      for ((id, body) in events) {
        insert(id.seqno, id.site, body)
      }
    }

/**
 * Creates a new [MutableEventLog], with no aggregate.
 *
 * @param events some events to pre-populate the history.
 * @param clock the [Clock] used to generate new events.
 */
public fun mutableTreeEventLogOf(
    vararg events: Pair<EventIdentifier, ByteArray>,
    clock: Clock = Clock.System,
): MutableEventLog =
    TreeMutableEventLog(clock).apply {
      for ((id, body) in events) {
        insert(id.seqno, id.site, body)
      }
    }

/**
 * Creates a new [MutableEventLog], with no aggregate.
 *
 * @param events some events to pre-populate the history.
 * @param clock the [Clock] used to generate new events.
 */
public fun mutableEventLogOf(
    vararg events: Event,
    clock: Clock = Clock.System,
): MutableEventLog = LinearMutableEventLog(clock).apply { for (event in events) insert(event) }

/**
 * Creates a new [MutableEventLog], with no aggregate.
 *
 * @param events some events to pre-populate the history.
 * @param clock the [Clock] used to generate new events.
 */
public fun mutableTreeEventLogOf(
    vararg events: Event,
    clock: Clock = Clock.System,
): MutableEventLog = TreeMutableEventLog(clock).apply { for (event in events) insert(event) }
