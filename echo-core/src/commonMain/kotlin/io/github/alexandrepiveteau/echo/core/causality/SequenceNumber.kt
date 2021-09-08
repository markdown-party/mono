package io.github.alexandrepiveteau.echo.core.causality

import kotlin.jvm.JvmInline
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * A sequence number is a monotonically increasing value defined for a site. It can be seen as a
 * logical timestamp.
 *
 * @constructor creates a [SequenceNumber], using a backing value.
 * @param index the sequence number for this [SequenceNumber].
 */
@Serializable(with = SequenceNumberSerializer::class)
@JvmInline
value class SequenceNumber
internal constructor(
    @PublishedApi internal val index: UInt,
) {

  /** Increments this value. */
  operator fun inc(): SequenceNumber {
    return SequenceNumber(index.inc())
  }

  /** Adds a certain [count] to this value. */
  operator fun plus(count: UInt): SequenceNumber {
    return SequenceNumber(index + count)
  }

  /** Compares this [SequenceNumber] with an other [SequenceNumber]. */
  operator fun compareTo(other: SequenceNumber): Int {
    return index.compareTo(other.index)
  }

  companion object {

    /**
     * A sentinel value used to initialize a non-null parameter. It also has the property of being
     * the smallest [SequenceNumber], so [maxOf] will always return the other choice.
     */
    val Unspecified = SequenceNumber(UInt.MIN_VALUE)

    /**
     * The minimum possible value for a [SequenceNumber]. Events may not be assigned a value smaller
     * than that.
     */
    val Min = SequenceNumber(UInt.MIN_VALUE + 1U)

    /** The maximum possible value for a [SequenceNumber]. */
    val Max = SequenceNumber(UInt.MAX_VALUE)
  }
}

/** Returns the maximum [SequenceNumber] in a pair. */
fun maxOf(a: SequenceNumber, b: SequenceNumber): SequenceNumber = if (a >= b) a else b

/** Returns the maximum [SequenceNumber] in a pair. */
fun maxOf(a: SequenceNumber, b: SequenceNumber, c: SequenceNumber): SequenceNumber =
    maxOf(a, maxOf(b, c))

/** `false` when this is [SequenceNumber.Unspecified]. */
inline val SequenceNumber.isSpecified: Boolean
  get() = index != SequenceNumber.Unspecified.index

/** `true` when this is [SequenceNumber.Unspecified]. */
inline val SequenceNumber.isUnspecified: Boolean
  get() = index == SequenceNumber.Unspecified.index

/** Creates a [SequenceNumber] from a [UInt]. */
fun UInt.toSequenceNumber(): SequenceNumber = SequenceNumber(this)

/** Creates a [UInt] from a [SequenceNumber]. */
fun SequenceNumber.toUInt(): UInt = index

fun Instant.toSequenceNumber(): SequenceNumber {
  // TODO : Not naive sequence numbers.
  return SequenceNumber(this.epochSeconds.toUInt())
}
