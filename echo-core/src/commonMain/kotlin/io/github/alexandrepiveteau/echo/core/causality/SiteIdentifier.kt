package io.github.alexandrepiveteau.echo.core.causality

import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlin.random.nextUInt

/**
 * A [SiteIdentifier] is a unique identifier amongst all the sites in a distributed system. Two
 * sites are guaranteed to have different [SiteIdentifier].
 */
@JvmInline
value class SiteIdentifier
internal constructor(
    @PublishedApi internal val unique: UInt,
) : Comparable<SiteIdentifier> {

  /** Compares this [SiteIdentifier] with an other [SiteIdentifier]. */
  override operator fun compareTo(other: SiteIdentifier): Int {
    return unique.compareTo(other.unique)
  }

  companion object {

    /** The minimum [SiteIdentifier] that could possibly exist. */
    val Min: SiteIdentifier = SiteIdentifier(1U)

    /** The maximum [SiteIdentifier] that could possibly exist. */
    val Max: SiteIdentifier = SiteIdentifier(UInt.MAX_VALUE)

    /**
     * A special sentinel value that indicates that no [SiteIdentifier] has been specified. This
     * should be used when an optional [SiteIdentifier] is expected and can't be provided.
     *
     * Using a dedicated value rather than an optional avoids auto-boxing.
     */
    val Unspecified: SiteIdentifier = SiteIdentifier(0U)
  }
}

/** `false` when this is [SiteIdentifier.Unspecified]. */
inline val SiteIdentifier.isSpecified: Boolean
  get() = unique != SiteIdentifier.Unspecified.unique

/** `true` when this is [SiteIdentifier.Unspecified]. */
inline val SiteIdentifier.isUnspecified: Boolean
  get() = unique == SiteIdentifier.Unspecified.unique

/**
 * If this [SiteIdentifier] [isSpecified] then this is returned, otherwise [block] is executed and
 * its result is returned.
 */
inline fun SiteIdentifier.takeOrElse(block: () -> SiteIdentifier): SiteIdentifier =
    if (isSpecified) this else block()

/** Creates a [SiteIdentifier] from the current [Int]. */
fun UInt.toSiteIdentifier(): SiteIdentifier {
  return SiteIdentifier(this)
}

/** Creates an [UInt] from the current [SiteIdentifier]. */
fun SiteIdentifier.toUInt(): UInt {
  return this.unique
}

/**
 * Gets the next random [SiteIdentifier] from the random number generator. It is guaranteed not to
 * be [SiteIdentifier.Unspecified].
 */
fun Random.nextSiteIdentifier(): SiteIdentifier {
  // In range [0, UInt.MAX_VALUE), then shifted right by one.
  return SiteIdentifier(nextUInt(until = UInt.MAX_VALUE) + 1U)
}
