package io.github.alexandrepiveteau.echo.core

import kotlin.jvm.JvmInline
import kotlin.random.Random

/**
 * A [SiteIdentifier] is a unique identifier amongst all the sites in a distributed system. Two
 * sites are guaranteed to have different [SiteIdentifier].
 */
@JvmInline
value class SiteIdentifier
internal constructor(
  internal val unique: Int,
) {
  companion object {

    /**
     * A special sentinel value that indicates that no [SiteIdentifier] has been specified. This
     * should be used when an optional [SiteIdentifier] is expected and can't be provided.
     *
     * Using a dedicated value rather than an optional avoids auto-boxing.
     */
    val None: SiteIdentifier = SiteIdentifier(Int.MAX_VALUE)
  }
}

/** Creates a [SiteIdentifier] from the current [Int]. */
fun Int.toSiteIdentifier(): SiteIdentifier {
  return SiteIdentifier(this)
}

/** Creates an [Int] from the current [SiteIdentifier]. */
fun SiteIdentifier.toInt(): Int {
  return this.unique
}

/**
 * Gets the next random [SiteIdentifier] from the random number generator. It is guaranteed not to
 * be [SiteIdentifier.None].
 */
fun Random.nextSiteIdentifier(): SiteIdentifier {
  return SiteIdentifier(nextInt(until = Int.MAX_VALUE))
}
