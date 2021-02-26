package markdown.echo.causal

import kotlin.random.Random

/**
 * A [SiteIdentifier] is globally unique amongst all the sites in the distributed system.
 *
 * TODO : The current implementation has 32-bits of entropy; this is usually not sufficient, but
 *        will be changed at a later time.
 */
inline class SiteIdentifier internal constructor(
    internal val unique: Int,
) {
    companion object {

        /**
         * Generates a randomly and uniformly picked [SiteIdentifier].
         */
        fun random(): SiteIdentifier = SiteIdentifier(unique = Random.nextInt())
    }
}
