package io.github.alexandrepiveteau.echo.core.buffer

/** Pushes the given [Byte] at the current gap cursor. */
fun MutableByteGapBuffer.pushAtGap(
    value: Byte,
) =
    push(
        value = value,
        offset = gap.startIndex,
    )

/** Pushes an array of [Byte] at the current gap cursor. */
fun MutableByteGapBuffer.pushAtGap(
    array: ByteArray,
    from: Int = 0,
    until: Int = array.size,
) =
    push(
        array = array,
        offset = gap.startIndex,
        startIndex = from,
        endIndex = until,
    )
