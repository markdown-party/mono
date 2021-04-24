package io.github.alexandrepiveteau.echo.site

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier.Companion.random
import io.github.alexandrepiveteau.echo.link
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.github.alexandrepiveteau.echo.suspendTest
import io.github.alexandrepiveteau.echo.sync
import kotlin.test.Test

class MutableSiteSyncTest {

  @Test
  fun emptyLink_terminates() = suspendTest {
    val site = mutableSite<Nothing>(random())
    val link = link<Incoming<Nothing>, Outgoing<Nothing>> {}
    sync(site.incoming(), link)
  }
}
