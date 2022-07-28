package io.github.alexandrepiveteau.echo.site

import io.github.alexandrepiveteau.echo.SyncStrategy.Continuous
import io.github.alexandrepiveteau.echo.SyncStrategy.Once
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.sync
import kotlin.random.Random
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class MutableSiteSyncTest {

  @Test
  fun syncOnce_noProjection_terminates() = runTest {
    val alice = mutableSite<Unit>(Random.nextSiteIdentifier(), strategy = Once)
    val bob = mutableSite<Unit>(Random.nextSiteIdentifier(), strategy = Once)

    alice.event { yield(Unit) }
    sync(alice, bob)
    alice.event { yield(Unit) }
    bob.event { yield(Unit) }
    sync(alice, bob)
  }

  @Test
  // Bug reproducer
  fun syncOnce_projection_terminates() = runTest {
    val alice =
        mutableSite<Unit>(
            identifier = Random.nextSiteIdentifier(),
            strategy = Continuous,
        )
    val bob =
        mutableSite<Unit>(
            identifier = Random.nextSiteIdentifier(),
            strategy = Once,
        )

    alice.event { yield(Unit) }
    sync(alice, bob)
    alice.event { yield(Unit) }
    // bob.event { yield(Unit) }
    sync(alice, bob)
  }
}
