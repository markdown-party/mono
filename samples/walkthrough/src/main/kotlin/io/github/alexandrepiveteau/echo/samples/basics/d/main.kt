package io.github.alexandrepiveteau.echo.samples.basics.d

import io.github.alexandrepiveteau.echo.SyncStrategy.Once
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.projections.ChangeScope
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection
import io.github.alexandrepiveteau.echo.sync
import kotlin.random.Random

object TwoWaySet : TwoWayProjection<MutableSet<Int>, Int, Int> {

  override fun ChangeScope<Int>.forward(
      model: MutableSet<Int>,
      id: EventIdentifier,
      event: Int
  ): MutableSet<Int> {
    if (model.add(event)) push(event) // record the change; in this case, the inserted event
    return model
  }

  override fun backward(
      model: MutableSet<Int>,
      id: EventIdentifier,
      event: Int,
      change: Int
  ): MutableSet<Int> {
    return model.apply { remove(change) } // revert the change
  }
}

suspend fun main() {
  val alice =
      mutableSite(
          identifier = Random.nextSiteIdentifier(),
          initial = mutableSetOf(),
          projection = TwoWaySet,
          transform = MutableSet<Int>::toSet,
          strategy = Once,
      )

  val bob =
      mutableSite(
          identifier = Random.nextSiteIdentifier(),
          initial = mutableSetOf(),
          projection = TwoWaySet,
          transform = MutableSet<Int>::toSet,
          strategy = Once,
      )

  alice.event { yieldAll(listOf(1, 2, 3)) }
  bob.event { yieldAll(listOf(3, 4)) }

  sync(alice, bob)

  alice.event { yield(5) }

  sync(alice, bob)

  println("alice : ${alice.value}")
  println("bob : ${bob.value}")
}
