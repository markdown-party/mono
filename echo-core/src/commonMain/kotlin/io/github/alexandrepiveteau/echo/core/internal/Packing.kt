@file:Suppress("NOTHING_TO_INLINE")

package io.github.alexandrepiveteau.echo.core.internal

// BYTE <-> INT conversions

/** Unpacks the first [Byte] from a given [Int]. */
internal inline fun unpackByte1(value: Int): Byte = (value shr 24).toByte()

/** Unpacks the second [Byte] from a given [Int]. */
internal inline fun unpackByte2(value: Int): Byte = (value shr 16).toByte()

/** Unpacks the third [Byte] from a given [Int]. */
internal inline fun unpackByte3(value: Int): Byte = (value shr 8).toByte()

/** Unpacks the fourth [Byte] from a given [Int]. */
internal inline fun unpackByte4(value: Int): Byte = value.and(0xFF).toByte()

/** Unpacks the fifth [Byte] from a given [Int]. */
internal inline fun packBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Int {
  return (b1.toInt().and(0xFF) shl 24) or
      (b2.toInt().and(0xFF) shl 16) or
      (b3.toInt().and(0xFF) shl 8) or
      b4.toInt().and(0xFF)
}
