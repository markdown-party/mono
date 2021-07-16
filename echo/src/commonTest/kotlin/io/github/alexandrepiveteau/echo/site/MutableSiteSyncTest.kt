package io.github.alexandrepiveteau.echo.site

import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.link
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.github.alexandrepiveteau.echo.suspendTest
import io.github.alexandrepiveteau.echo.sync
import io.github.alexandrepiveteau.echo.sync.SyncStrategy.Companion.Continuous
import io.github.alexandrepiveteau.echo.sync.SyncStrategy.Companion.Once
import kotlin.random.Random
import kotlin.test.Test

class MutableSiteSyncTest {

  @Test
  fun emptyLink_terminates() = suspendTest {
    val site = mutableSite<Unit>(Random.nextSiteIdentifier())
    val link = link<Incoming, Outgoing> {}
    sync(site.incoming(), link)
  }

  @Test
  fun syncOnce_noProjection_terminates() = suspendTest {
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
  fun syncOnce_projection_terminates() = suspendTest {
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
