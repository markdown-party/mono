package io.github.alexandrepiveteau.echo.pn

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.projections.ChangeScope
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection

/** A [TwoWayProjection] which models a counter with additions and deletions. */
object TwoWayPNCounter : TwoWayProjection<Int, Int, Int> {

  override fun ChangeScope<Int>.forward(model: Int, id: EventIdentifier, event: Int): Int {
    push(event)
    return model + event
  }

  override fun backward(model: Int, id: EventIdentifier, event: Int, change: Int): Int {
    return model - change
  }
}
