package io.github.alexandrepiveteau.echo.core.causality

import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlinx.serialization.Serializable

/**
 * A [SiteIdentifier] is a unique identifier amongst all the sites in a distributed system. Two
 * sites are guaranteed to have different [SiteIdentifier].
 */
@Serializable(with = SiteIdentifierSerializer::class)
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
  }
}

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
