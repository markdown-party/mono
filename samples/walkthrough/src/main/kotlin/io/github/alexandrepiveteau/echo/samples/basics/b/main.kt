package io.github.alexandrepiveteau.echo.samples.basics.b

import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.samples.basics.a.GCounter
import io.github.alexandrepiveteau.echo.sync
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

suspend fun main(): Unit = coroutineScope {
  val bob = mutableSite(Random.nextSiteIdentifier(), 0, GCounter)
  val carol = mutableSite(Random.nextSiteIdentifier(), 0, GCounter)

  // Start a coroutine that regularly emits new events with bob.
  val j1 = launch {
    while (true) {
      bob.event { state -> yield(state + 1) }
      delay(1000)
    }
  }

  // Collect and observe the state of carol.
  val j2 = launch { carol.value.collect { println("Value $it") } }

  // Continuously sync bob and carol.
  withTimeoutOrNull(10_000) { sync(bob, carol) }

  j1.cancel()
  j2.cancel()
}
