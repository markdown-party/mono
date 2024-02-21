package io.github.alexandrepiveteau.echo.core.buffer

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray

/** Pushes the given [EventIdentifier] at the current gap cursor. */
public fun MutableEventIdentifierGapBuffer.pushAtGap(
    value: EventIdentifier,
): Unit =
    push(
        value = value,
        offset = gap.startIndex,
    )

/** Pushes an array of [EventIdentifier] at the current gap cursor. */
public fun MutableEventIdentifierGapBuffer.pushAtGap(
    array: EventIdentifierArray,
    from: Int = 0,
    until: Int = array.size,
): Unit =
    push(
        array = array,
        offset = gap.startIndex,
        startIndex = from,
        endIndex = until,
    )
