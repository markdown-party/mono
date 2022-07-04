package io.github.alexandrepiveteau.echo.site

import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.sync
import io.github.alexandrepiveteau.echo.sync.SyncStrategy
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class MutableSiteYieldTest {

  @Test
  fun sequential_yields_areOrdered() = runTest {
    val alice = mutableSite<Int>(Random.nextSiteIdentifier(), strategy = SyncStrategy.Once)
    val bob = mutableSite<Int>(Random.nextSiteIdentifier(), strategy = SyncStrategy.Once)
    val a = alice.event { yield(123) }
    sync(alice, bob)
    val b = bob.event { yield(123) }
    assertTrue(a < b)
  }
}
