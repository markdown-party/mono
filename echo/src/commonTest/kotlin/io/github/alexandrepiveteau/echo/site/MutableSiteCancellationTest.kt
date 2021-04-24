package io.github.alexandrepiveteau.echo.site

import io.github.alexandrepiveteau.echo.buffer
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier.Companion.random
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList

class MutableSiteCancellationTest {

  @Test
  fun unbuffered_site_doesNot_advertise() = suspendTest {
    val site =
        mutableSite<Int>(identifier = random())
            .apply { event { yield(123) } }
            .buffer(Channel.RENDEZVOUS)
    val result = site.incoming().talk(emptyFlow()).toList()
    assertEquals(emptyList(), result)
  }
}
