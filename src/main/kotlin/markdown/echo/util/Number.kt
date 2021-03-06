@file:Suppress("NOTHING_TO_INLINE")

package markdown.echo.util

/**
 * Adds two [Int], bounding the result in [Int.MIN_VALUE] and [Int.MAX_VALUE] on overflow.
 *
 * @see Math.addExact the implementation describes overflow detection conditions
 */
internal inline fun Int.plusBoundOverflows(other: Int): Int {
  val result = this + other
  if (result > 0 && this < 0 && other < 0) return Int.MAX_VALUE
  if (result < 0 && this > 0 && other > 0) return Int.MIN_VALUE
  return result
}

/** Adds two [UInt], bounding the result to [UInt.MAX_VALUE] on overflow. */
@ExperimentalUnsignedTypes
internal inline fun UInt.plusBoundOverflows(other: UInt): UInt {
  val result = this + other
  if (result < this || result < other) return UInt.MAX_VALUE
  return result
}

/**
 * Adds two [Long], bounding the result in [Long.MIN_VALUE] and [Long.MAX_VALUE] on overflow.
 *
 * @see Math.addExact the implementation describes overflow detection conditions
 */
internal inline fun Long.plusBoundOverflows(other: Long): Long {
  val result = this + other
  if (result > 0 && this < 0 && other < 0) return Long.MAX_VALUE
  if (result < 0 && this > 0 && other > 0) return Long.MIN_VALUE
  return result
}

/** Adds two [ULong], bounding the result to [ULong.MAX_VALUE] on overflow. */
@ExperimentalUnsignedTypes
internal inline fun ULong.plusBoundOverflows(other: ULong): ULong {
  val result = this + other
  if (result < this || result < other) return ULong.MAX_VALUE
  return result
}

// PACKING UTILITIES

/** Packs two Int values into one Long value for use in inline classes. */
internal inline fun packInts(val1: Int, val2: Int): Long {
  return val1.toLong().shl(32) or (val2.toLong() and 0xFFFFFFFF)
}

/** Unpacks the first Int value in [packInts] from its returned Long. */
internal inline fun unpackInt1(value: Long): Int {
  return value.shr(32).toInt()
}

/** Unpacks the second Int value in [packInts] from its returned Long. */
internal inline fun unpackInt2(value: Long): Int {
  return value.and(0xFFFFFFFF).toInt()
}
