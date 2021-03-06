package markdown.echo.causal

import markdown.echo.util.plusBoundOverflows

/**
 * A [SequenceNumber] is a logical timestamp. The implementation won't overflow, and is clamped
 * between [SequenceNumber.Zero] and [SequenceNumber.Max].
 *
 * Increasing sequence numbers do not imply a causality relationship, but a causality relationship
 * implies increasing sequence numbers.
 */
inline class SequenceNumber
internal constructor(
    internal val index: UInt,
) : Comparable<SequenceNumber> {
  operator fun inc(): SequenceNumber = plus(1U)
  operator fun plus(steps: UInt): SequenceNumber = SequenceNumber(index.plusBoundOverflows(steps))
  override fun compareTo(other: SequenceNumber) = index.compareTo(other.index)

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
