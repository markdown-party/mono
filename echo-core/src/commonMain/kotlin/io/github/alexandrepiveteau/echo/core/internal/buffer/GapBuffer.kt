package io.github.alexandrepiveteau.echo.core.internal.buffer

import kotlin.RequiresOptIn.Level.WARNING

/** The default capacity for an empty gap buffer. */
internal const val DefaultGapBufferSize = 32

/**
 * An annotation that marks delicate gap buffer APIs, which may be tricking to use properly and
 * might easily lead to mis-usage.
 */
@MustBeDocumented
@RequiresOptIn(level = WARNING, message = "This API is low-level and requires cautious usage.")
internal annotation class DelicateGapBufferApi

/** Transforms the given [Array] buffer with the provided [gap] into a [String]. */
internal fun <T> bufferToString(
    buffer: Array<T>,
    gap: IntRange,
): String = buildString {
  var index = 0
  while (index < buffer.size) {
    if (index in gap) append('*')
    append(buffer[index])
    if (index != buffer.size - 1) append(", ")
    index++
  }
}
