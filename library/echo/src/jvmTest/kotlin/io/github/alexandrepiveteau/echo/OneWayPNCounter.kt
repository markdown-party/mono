package io.github.alexandrepiveteau.echo

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import kotlin.math.max
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate

object OneWayPNCounter : OneWayProjection<PersistentMap<SiteIdentifier, Int>, Int> {

  override fun forward(
      model: PersistentMap<SiteIdentifier, Int>,
      identifier: EventIdentifier,
      event: Int,
  ): PersistentMap<SiteIdentifier, Int> =
      model.mutate {
        val (_, site) = identifier
        val current = it.getOrElse(site) { event }
        it[site] = max(current, event)
      }
}
