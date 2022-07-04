package io.github.alexandrepiveteau.echo.core.buffer

/** Pushes the given [Int] at the current gap cursor. */
fun MutableIntGapBuffer.pushAtGap(
    value: Int,
) =
    push(
        value = value,
        offset = gap.startIndex,
    )

/** Pushes an array of [Int] at the current gap cursor. */
fun MutableIntGapBuffer.pushAtGap(
    array: IntArray,
    from: Int = 0,
    until: Int = array.size,
) =
    push(
        array = array,
        offset = gap.startIndex,
        startIndex = from,
        endIndex = until,
    )
