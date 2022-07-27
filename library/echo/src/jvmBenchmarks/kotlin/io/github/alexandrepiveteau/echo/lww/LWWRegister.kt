package io.github.alexandrepiveteau.echo.lww

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.projections.OneWayProjection

/**
 * A [OneWayProjection] which represents a [LWWRegister], which will preserve the last value in the
 * operation log.
 *
 * @param T the type of the events.
 */
class LWWRegister<T> : OneWayProjection<LWWRegister.Tagged<T>, T> {

  /** A tagged [T] with an [EventIdentifier]. */
  data class Tagged<out T>(val value: T, val identifier: EventIdentifier)

  override fun forward(
      model: Tagged<T>,
      identifier: EventIdentifier,
      event: T,
  ) = if (model.identifier >= identifier) model else Tagged(event, identifier)
}
