package io.github.alexandrepiveteau.echo.samples.basics.c

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import io.github.alexandrepiveteau.echo.sync
import io.github.alexandrepiveteau.echo.sync.SyncStrategy.Companion.Once
import kotlin.random.Random

object GSet : OneWayProjection<Set<Int>, Int> {
  override fun forward(model: Set<Int>, identifier: EventIdentifier, event: Int): Set<Int> {
    return model + event
  }
}

suspend fun main() {
  val alice = mutableSite(Random.nextSiteIdentifier(), emptySet(), GSet, strategy = Once)
  val bob = mutableSite(Random.nextSiteIdentifier(), emptySet(), GSet, strategy = Once)

  alice.event {
    yield(1)
    yield(2)
    yield(3)
  }
  bob.event { yieldAll(listOf(3, 4)) }

  sync(alice, bob)

  println("alice : ${alice.value}")
  println("bob : ${bob.value}")
}
