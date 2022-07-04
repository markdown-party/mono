package io.github.alexandrepiveteau.echo.site

import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.sync
import io.github.alexandrepiveteau.echo.sync.SyncStrategy
import kotlin.test.Test
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest

class MutableSiteAdvertisementTest {

  @Test
  fun threeSites_interleaving() = runTest {
    withContext(Dispatchers.Default) {
      val alice = (1U).toSiteIdentifier()
      val bob = (2U).toSiteIdentifier()
      val carol = (3U).toSiteIdentifier()
      val david = (4U).toSiteIdentifier()
      val eve = (5U).toSiteIdentifier()

      val aliceExchange = mutableSite<Unit>(alice, strategy = SyncStrategy.Once)
      val bobExchange = mutableSite<Unit>(bob, strategy = SyncStrategy.Once)
      val carolExchange = mutableSite<Unit>(carol, strategy = SyncStrategy.Once)
      val davidExchange = mutableSite<Unit>(david, strategy = SyncStrategy.Once)
      val eveExchange = mutableSite<Unit>(eve, strategy = SyncStrategy.Once)

      aliceExchange.event { yield(Unit) }
      bobExchange.event { yield(Unit) }
      carolExchange.event { yield(Unit) }
      davidExchange.event { yield(Unit) }
      eveExchange.event { yield(Unit) }

      // Alice and Carol know 1 _ 3 _
      sync(aliceExchange, carolExchange)
      // Skip bob sync(aliceExchange, bobExchange)
      sync(aliceExchange, carolExchange)
      sync(aliceExchange, davidExchange)
      sync(aliceExchange, eveExchange)
      sync(bobExchange, eveExchange)

      // Alice knows 1 _ 3 4 5
      // Eve knows 1 2 3 4 5

      val x = mutableSite<Unit>((6u).toSiteIdentifier())
      val y = mutableSite<Unit>((7u).toSiteIdentifier())

      // Timeout of 1000ms, sufficient to wait the 500ms + 500ms to let the crash manifest.
      launch { withTimeout(500) { sync(x, y) } }

      delay(500) // Let them finish their initial exchange.
      sync(x, aliceExchange)
      sync(y, eveExchange) // crash
    }
  }
}
