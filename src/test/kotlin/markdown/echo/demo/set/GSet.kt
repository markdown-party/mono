package markdown.echo.demo.set

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import markdown.echo.causal.SiteIdentifier
import markdown.echo.mutableSite
import markdown.echo.projections.OneWayProjection
import markdown.echo.projections.projection
import markdown.echo.sync
import kotlin.test.Test
import kotlin.test.assertEquals

private sealed class GSetEvent<out T> {
  data class Add<out T>(val item: T) : GSetEvent<T>()
}

private fun <T> gSetProjection() =
    OneWayProjection<Set<T>, GSetEvent<T>> { event, model ->
      when (event) {
        // Add events, and duplicate insertions.
        is GSetEvent.Add -> model + event.item
      }
    }

class GSetTest {

  @Test
  fun `one site can create a set and create new events`() = runBlocking {
    val alice = SiteIdentifier.random()
    val echo = mutableSite<GSetEvent<Int>>(identifier = alice)

    echo.event {
      yield(GSetEvent.Add(1))
      yield(GSetEvent.Add(2))
      yield(GSetEvent.Add(3))
    }

    val result = echo.projection(emptySet(), gSetProjection()).first { it.size == 3 }
    assertEquals(setOf(1, 2, 3), result)
  }

  @Test
  fun `two sites can create a shared set and eventually sync`() = runBlocking {
    // Create Alice, our first site.
    val alice = SiteIdentifier.random()
    val aliceEcho = mutableSite<GSetEvent<Int>>(identifier = alice)

    // Create Bob, our second site.
    val bob = SiteIdentifier.random()
    val bobEcho = mutableSite<GSetEvent<Int>>(identifier = bob)

    // Alice adds the elements 1 and 2.
    aliceEcho.event {
      yield(GSetEvent.Add(1))
      yield(GSetEvent.Add(2))
    }

    // Bob adds the elements 2, 3 and 4 concurrently.
    bobEcho.event {
      yield(GSetEvent.Add(2))
      yield(GSetEvent.Add(3))
      yield(GSetEvent.Add(4))
    }

    val aliceBeforeSync = aliceEcho.projection(emptySet(), gSetProjection()).first { it.size == 2 }
    val bobBeforeSync = bobEcho.projection(emptySet(), gSetProjection()).first { it.size == 3 }

    // Before sync, both sites have not merged their operations yet.
    assertEquals(setOf(1, 2), aliceBeforeSync)
    assertEquals(setOf(2, 3, 4), bobBeforeSync)

    // Sync both sites (with a timeout, since by default they'll keep the connection open until
    // either side cancels).
    try {
      withTimeout(timeMillis = 1000) { sync(aliceEcho, bobEcho) }
    } catch (expect: TimeoutCancellationException) {}

    // Finally, look at the resulting set of both sites.
    val aliceAfterSync = aliceEcho.projection(emptySet(), gSetProjection()).first { it.size == 4 }
    val bobAfterSync = aliceEcho.projection(emptySet(), gSetProjection()).first { it.size == 4 }

    // All the events are properly synced.
    val expected = setOf(1, 2, 3, 4)
    assertEquals(expected, aliceAfterSync)
    assertEquals(expected, bobAfterSync)
  }
}
