package io.github.alexandrepiveteau.echo.causal

import io.github.alexandrepiveteau.echo.util.plusBoundOverflows
import kotlin.jvm.JvmInline

/**
 * A [SequenceNumber] is a logical timestamp. The implementation won't overflow, and is clamped
 * between [SequenceNumber.Zero] and [SequenceNumber.Max].
 *
 * Increasing sequence numbers do not imply a causality relationship, but a causality relationship
 * implies increasing sequence numbers.
 */
@JvmInline
value class SequenceNumber
internal constructor(
    internal val index: UInt,
) : Comparable<SequenceNumber> {
  operator fun inc(): SequenceNumber = plus(1U)
  operator fun plus(steps: UInt): SequenceNumber = SequenceNumber(index.plusBoundOverflows(steps))
  override fun compareTo(other: SequenceNumber) = index.compareTo(other.index)

  override fun toString(): String = "SequenceNumber(index = $index)"

  companion object {

    /** The base [SequenceNumber], that is expected when no events have been issued yet. */
    val Zero: SequenceNumber = SequenceNumber(UInt.MIN_VALUE)

    /**
     * The maximum value that a [SequenceNumber] may have. A site may not issue sequence numbers
     * higher than this value.
     */
    val Max: SequenceNumber = SequenceNumber(UInt.MAX_VALUE)
  }
}

fun UInt.toSequenceNumber(): SequenceNumber = SequenceNumber(index = this)

fun SequenceNumber.toUInt(): UInt = index
