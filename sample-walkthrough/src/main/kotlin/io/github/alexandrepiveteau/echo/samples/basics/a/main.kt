package io.github.alexandrepiveteau.echo.samples.basics.a

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import io.github.alexandrepiveteau.echo.sync
import io.github.alexandrepiveteau.echo.sync.SyncStrategy
import kotlin.random.Random

val GCounter = OneWayProjection { model: Int, _: EventIdentifier, event: Int ->
  maxOf(model, event)
}

suspend fun main() {

  // create two distinct actors that manage a g-counter
  val alice = mutableSite(Random.nextSiteIdentifier(), 0, GCounter, strategy = SyncStrategy.Once)
  val bob = mutableSite(Random.nextSiteIdentifier(), 0, GCounter)

  alice.event { // this : EventScope<Int>
    yield(42) // emit a new value to the g-counter
  }

  println("alice ${alice.value}, bob ${bob.value}")

  sync(alice, bob) // let both sites converge
  println("synced !")

  println("alice ${alice.value}, bob ${bob.value}")
}
