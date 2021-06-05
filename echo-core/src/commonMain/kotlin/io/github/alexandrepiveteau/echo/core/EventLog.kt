package io.github.alexandrepiveteau.echo.core

import io.github.alexandrepiveteau.echo.core.internal.ByteGapBuffer
import io.github.alexandrepiveteau.echo.core.internal.EventIdentifierGapBuffer
import io.github.alexandrepiveteau.echo.core.internal.IntGapBuffer

/**
 * An [EventLog] is a high-performance mutable list of serialized events, which are concatenated one
 * after each other in a contiguous [ByteArray]. An [EventLog] is optimized for consecutive
 * insertions and removals at the same index; this works particularly well when many events are
 * appended to the end of the [EventLog].
 */
class EventLog {

  /** The [ByteGapBuffer] in which the events are individually managed. */
  private val events = ByteGapBuffer()
  private val identifiers = EventIdentifierGapBuffer()
  private val sizes = IntGapBuffer()

  init {}

  // /**
  //  * Merges the provided [EventLog] in the current [EventLog]. This method takes a linear time in
  //  * the amount of shared operations.
  //  */
  // fun merge(from: EventLog) {}

  /** Clears this [EventLog], removing all the contained operations. */
  fun clear() {
    events.clear()
    identifiers.clear()
    sizes.clear()
  }
}
