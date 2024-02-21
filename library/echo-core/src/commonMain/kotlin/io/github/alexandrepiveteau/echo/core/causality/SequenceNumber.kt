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
public value class SequenceNumber
internal constructor(
    @PublishedApi internal val index: UInt,
) : Comparable<SequenceNumber> {

  /** Increments this value. */
  public operator fun inc(): SequenceNumber {
    if (isUnspecified) return Unspecified // No-op on Unspecified values.
    return SequenceNumber(index.inc())
  }

  /** Adds a certain [count] to this value. */
  public operator fun plus(count: UInt): SequenceNumber {
    if (isUnspecified) return Unspecified // No-op on Unspecified values.
    return SequenceNumber(index + count)
  }

  /** Compares this [SequenceNumber] with another [SequenceNumber]. */
  override operator fun compareTo(other: SequenceNumber): Int {
    return index.compareTo(other.index)
  }

  override fun toString(): String = if (isUnspecified) "Unspecified" else index.toString()

  public companion object {

    /**
     * A sentinel value used to initialize a non-null parameter. It also has the property of being
     * the smallest [SequenceNumber], so [maxOf] will always return the other choice.
     */
    public val Unspecified: SequenceNumber = SequenceNumber(UInt.MIN_VALUE)

    /**
     * The minimum possible value for a [SequenceNumber]. Events may not be assigned a value smaller
     * than that.
     */
    public val Min: SequenceNumber = SequenceNumber(UInt.MIN_VALUE + 1U)

    /** The maximum possible value for a [SequenceNumber]. */
    public val Max: SequenceNumber = SequenceNumber(UInt.MAX_VALUE)
  }
}

/** Returns the maximum [SequenceNumber] in a pair. */
public fun maxOf(a: SequenceNumber, b: SequenceNumber): SequenceNumber = if (a >= b) a else b

/** Returns the maximum [SequenceNumber] in a triple. */
public fun maxOf(a: SequenceNumber, b: SequenceNumber, c: SequenceNumber): SequenceNumber =
    maxOf(a, maxOf(b, c))

/** `false` when this is [SequenceNumber.Unspecified]. */
public inline val SequenceNumber.isSpecified: Boolean
  get() = index != SequenceNumber.Unspecified.index

/** `true` when this is [SequenceNumber.Unspecified]. */
public inline val SequenceNumber.isUnspecified: Boolean
  get() = index == SequenceNumber.Unspecified.index

/**
 * If this [SequenceNumber] [isSpecified] then [this] is returned, otherwise [block] is executed and
 * its result is returned.
 */
public inline fun SequenceNumber.takeOrElse(
    block: () -> SequenceNumber,
): SequenceNumber = if (isSpecified) this else block()

/** Creates a [SequenceNumber] from a [UInt]. */
public fun UInt.toSequenceNumber(): SequenceNumber = SequenceNumber(this)

/** Creates a [UInt] from a [SequenceNumber]. */
public fun SequenceNumber.toUInt(): UInt = index

public fun Instant.toSequenceNumber(): SequenceNumber {
  // TODO : Not naive sequence numbers.
  return SequenceNumber(this.epochSeconds.toUInt())
}
