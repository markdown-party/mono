package io.github.alexandrepiveteau.echo.history2

import io.github.alexandrepiveteau.echo.logs.PersistentEventLog

/**
 * An interface that defines a [PersistentHistory]. A [PersistentHistory] contains the current
 * [model], and may also contain the previous changes performed on the model, making integration
 * with a [Merge] more efficient.
 *
 * @param M the type of the model.
 */
interface PersistentHistory<out M> {
  val model: M
}

/**
 * An interface defining how remote updates should be integrated in a local history of events.
 *
 * A [Merge] takes the current aggregate and its history of changes, and updates it with some newly
 * issued events coming from the remote sites. The merge operation should integrate all of the
 * remote changes at once.
 *
 * When implementing [Merge], you should consider the domain-specific optimizations that your
 * distributed data structures may or may not allow. For instance, some distributed registers will
 * benefit from commutative insertions, such as grow-only counters or sets.
 *
 * @param M the type of the aggregated model
 * @param T the type of the events
 * @param H the type of the history of changes
 */
fun interface Merge<out M, in T, H : PersistentHistory<M>> {

  // TODO : Remove changes from event log

  /** Merges the [events] log with the [history], computing the next [history] value. */
  fun integrate(history: H, events: PersistentEventLog<T, *>): H
}
