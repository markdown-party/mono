package io.github.alexandrepiveteau.echo.causal

import kotlin.random.Random

/**
 * A [SiteIdentifier] is globally unique amongst all the sites in the distributed system.
 *
 * TODO : Make sure there is enough entropy.
 */
inline class SiteIdentifier
constructor(
    internal val unique: Int,
) {

  override fun toString(): String = "SiteIdentifier(unique = $unique)"

  companion object {

    /** Generates a randomly and uniformly picked [SiteIdentifier]. */
    fun random(): SiteIdentifier = SiteIdentifier(unique = Random.nextInt())
  }
}

fun SiteIdentifier.toInt(): Int = unique
