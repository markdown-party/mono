package io.github.alexandrepiveteau.echo.core.buffer

/** Pushes the given [Char] at the current gap cursor. */
fun MutableCharGapBuffer.pushAtGap(
    value: Char,
) =
    push(
        value = value,
        offset = gap.startIndex,
    )

/** Pushes an array of [Char] at the current gap cursor. */
fun MutableCharGapBuffer.pushAtGap(
    array: CharArray,
    from: Int = 0,
    until: Int = array.size,
) =
    push(
        array = array,
        offset = gap.startIndex,
        startIndex = from,
        endIndex = until,
    )
