package io.github.alexandrepiveteau.echo

import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.sync.SyncStrategy
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/** All the test cases that can not run on Kotlin/JS and only target Kotlin/JVM for now. */
class Bugs {

  @Test
  fun sequential_yields_areOrdered() = suspendTest {
    val alice = mutableSite<Int>(Random.nextSiteIdentifier(), strategy = SyncStrategy.Once)
    val bob = mutableSite<Int>(Random.nextSiteIdentifier(), strategy = SyncStrategy.Once)
    // FIXME : Kotlin/JS does some unboxing when it shouldn't.
    val a = alice.event { yield(123) }
    sync(alice, bob)
    val b = bob.event { yield(123) }
    assertTrue(a < b)
  }
}
