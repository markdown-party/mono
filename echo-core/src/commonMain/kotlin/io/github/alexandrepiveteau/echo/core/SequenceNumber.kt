package io.github.alexandrepiveteau.echo.core

import kotlin.jvm.JvmInline

/**
 * A sequence number is a monotonically increasing value defined for a site. It can be seen as a
 * logical timestamp.
 *
 * @constructor creates a [SequenceNumber], using a backing value.
 * @param index the sequence number for this [SequenceNumber].
 */
@JvmInline
value class SequenceNumber
internal constructor(
  internal val index: UInt,
) {

  /** Increments this value. */
  operator fun inc(): SequenceNumber {
    return SequenceNumber(index.inc())
  }

  /** Compares this [SequenceNumber] with an other [SequenceNumber]. */
  operator fun compareTo(other: SequenceNumber): Int {
    return index.compareTo(other.index)
  }

  companion object {

    /** The minimum possible value for a [SequenceNumber]. */
    val MIN_VALUE = SequenceNumber(UInt.MIN_VALUE)

    /** The maximum possible value for a [SequenceNumber]. */
    val MAX_VALUE = SequenceNumber(UInt.MAX_VALUE)
  }
}

/** Creates a [SequenceNumber] from a [UInt]. */
fun UInt.toSequenceNumber(): SequenceNumber = SequenceNumber(this)

/** Creates a [UInt] from a [SequenceNumber]. */
fun SequenceNumber.toUInt(): UInt = index
