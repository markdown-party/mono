package io.github.alexandrepiveteau.echo.site

import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.link
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.github.alexandrepiveteau.echo.suspendTest
import io.github.alexandrepiveteau.echo.sync
import kotlin.random.Random
import kotlin.test.Test

class MutableSiteSyncTest {

  @Test
  fun emptyLink_terminates() = suspendTest {
    val site = mutableSite<Unit>(Random.nextSiteIdentifier())
    val link = link<Incoming, Outgoing> {}
    sync(site.incoming(), link)
  }
}
