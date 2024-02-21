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
public value class SiteIdentifier
internal constructor(
    @PublishedApi internal val unique: UInt,
) : Comparable<SiteIdentifier> {

  /** Compares this [SiteIdentifier] with an other [SiteIdentifier]. */
  override operator fun compareTo(other: SiteIdentifier): Int {
    return unique.compareTo(other.unique)
  }

  public companion object {

    /** The minimum [SiteIdentifier] that could possibly exist. */
    public val Min: SiteIdentifier = SiteIdentifier(UInt.MIN_VALUE)

    /** The maximum [SiteIdentifier] that could possibly exist. */
    public val Max: SiteIdentifier = SiteIdentifier(UInt.MAX_VALUE)
  }
}

/** Creates a [SiteIdentifier] from the current [Int]. */
public fun UInt.toSiteIdentifier(): SiteIdentifier {
  return SiteIdentifier(this)
}

/** Creates an [UInt] from the current [SiteIdentifier]. */
public fun SiteIdentifier.toUInt(): UInt {
  return this.unique
}

/** Gets the next random [SiteIdentifier] from the random number generator. */
public fun Random.nextSiteIdentifier(): SiteIdentifier {
  // In range [0, UInt.MAX_VALUE]
  return SiteIdentifier(nextUInt())
}
