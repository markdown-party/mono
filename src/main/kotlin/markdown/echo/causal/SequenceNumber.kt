package markdown.echo.causal

/**
 * A [SequenceNumber] is a logical timestamp.
 *
 * Increasing sequence numbers do not imply a causality relationship, but a causality relationship
 * implies increasing sequence numbers.
 */
inline class SequenceNumber internal constructor(
    internal val index: Int,
) : Comparable<SequenceNumber> {
    operator fun inc(): SequenceNumber = SequenceNumber(index + 1)
    override fun compareTo(other: SequenceNumber) = index.compareTo(other.index)
}
