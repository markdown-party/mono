@file:Suppress("NOTHING_TO_INLINE")

package io.github.alexandrepiveteau.echo.core

/** The default size for an empty [EventLog]. */
private const val DefaultEventLogSize = 32

inline fun unpackByte1(value: Int): Byte = (value shr 24).toByte()

inline fun unpackByte2(value: Int): Byte = (value shr 16).toByte()

inline fun unpackByte3(value: Int): Byte = (value shr 8).toByte()

inline fun unpackByte4(value: Int): Byte = value.and(0xFF).toByte()

inline fun packBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Int {
  return (b1.toInt().and(0xFF) shl 24) or
      (b2.toInt().and(0xFF) shl 16) or
      (b3.toInt().and(0xFF) shl 8) or
      b4.toInt().and(0xFF)
}

/**
 * An [EventLog] is a high-performance mutable list of serialized events, which are concatenated one
 * after each other in a contiguous [ByteArray]. An [EventLog] is optimized for consecutive
 * insertions and removals at the same index; this works particularly well when many events are
 * appended to the end of the [EventLog].
 */
class EventLog {

  /** The index at which new events should be inserted in the [events] array. */
  private var gapStart: Int = 0

  /** The index at which the insertion gap ends, non-inclusive. */
  private var gapEnd: Int = DefaultEventLogSize

  /** A contiguous sequence of bytes, representing the events in the log. */
  private var events = ByteArray(DefaultEventLogSize)
  // TODO : Store event identifiers.
}
