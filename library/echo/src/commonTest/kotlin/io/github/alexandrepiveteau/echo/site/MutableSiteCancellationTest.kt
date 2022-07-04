package io.github.alexandrepiveteau.echo.site

import io.github.alexandrepiveteau.echo.buffer
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class MutableSiteCancellationTest {

  @Test
  fun unbuffered_site_doesNot_advertise() = runTest {
    val site =
        mutableSite<Int>(identifier = Random.nextSiteIdentifier())
            .apply { event { yield(123) } }
            .buffer(Channel.RENDEZVOUS)
    val result = site.receive(emptyFlow()).toList()
    assertEquals(emptyList(), result)
  }
}
