package io.github.alexandrepiveteau.echo.core

import io.github.alexandrepiveteau.echo.core.internal.ByteGapBuffer

/**
 * An [EventLog] is a high-performance mutable list of serialized events, which are concatenated one
 * after each other in a contiguous [ByteArray]. An [EventLog] is optimized for consecutive
 * insertions and removals at the same index; this works particularly well when many events are
 * appended to the end of the [EventLog].
 */
class EventLog {

  /** The [ByteGapBuffer] in which the events are individually managed. */
  private val events = ByteGapBuffer()

  init {

  }
}
