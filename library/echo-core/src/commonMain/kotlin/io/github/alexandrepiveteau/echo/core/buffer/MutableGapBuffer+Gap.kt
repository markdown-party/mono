package io.github.alexandrepiveteau.echo.core.buffer

/** Pushes the given [T] at the current gap cursor. */
public fun <T> MutableGapBuffer<T>.pushAtGap(
    value: T,
): Unit =
    push(
        value = value,
        offset = gap.startIndex,
    )

/** Pushes an array of [T] at the current gap cursor. */
public fun <T> MutableGapBuffer<T>.pushAtGap(
    array: Array<T>,
    from: Int = 0,
    until: Int = array.size,
): Unit =
    push(
        array = array,
        offset = gap.startIndex,
        startIndex = from,
        endIndex = until,
    )
