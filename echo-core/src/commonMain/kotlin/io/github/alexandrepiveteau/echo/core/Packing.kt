@file:Suppress("NOTHING_TO_INLINE")

package io.github.alexandrepiveteau.echo.core

// INT <-> LONG conversions

/** Packs two Int values into one Long value for use in inline classes. */
@PublishedApi
internal fun packUInts(val1: UInt, val2: UInt): ULong {
  return val1.toULong().shl(32) or (val2.toULong() and 0xFFFFFFFFU)
}

/** Unpacks the first Int value in [packUInts] from its returned Long. */
@PublishedApi
internal fun unpackUInt1(value: ULong): UInt {
  return value.shr(32).toUInt()
}

/** Unpacks the second Int value in [packUInts] from its returned Long. */
@PublishedApi
internal fun unpackUInt2(value: ULong): UInt {
  return value.and(0xFFFFFFFFU).toUInt()
}
