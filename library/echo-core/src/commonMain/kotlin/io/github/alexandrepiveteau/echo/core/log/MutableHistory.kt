package io.github.alexandrepiveteau.echo.core.log

/**
 * A [MutableHistory] is a high-performance log of serialized events, which aggregate the events
 * into a [current] value which is incrementally computed from the linear sequence of events.
 *
 * @param T the type of the aggregate.
 *
 * @see MutableEventLog a variation of [MutableEventLog] with no incremental state
 */
public interface MutableHistory<out T> : History<T>, MutableEventLog
