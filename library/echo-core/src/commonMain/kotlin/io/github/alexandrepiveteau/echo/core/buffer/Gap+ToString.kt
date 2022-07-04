package io.github.alexandrepiveteau.echo.core.buffer

/**
 * Transforms the given [Array] buffer with this [Gap] into a [String].
 *
 * @param T the type of the items to display.
 * @receiver the [Gap] instance.
 */
internal fun <T> Gap.bufferToString(
    buffer: Array<T>,
): String = buildString {
  var index = 0
  while (index < buffer.size) {
    if (index in startIndex until endIndex) append('*')
    append(buffer[index])
    if (index != buffer.size - 1) append(", ")
    index++
  }
}
