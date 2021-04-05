package markdown.echo.demo.set

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import markdown.echo.causal.SiteIdentifier
import markdown.echo.logs.EventValue
import markdown.echo.mutableSite
import markdown.echo.projections.OneWayProjection
import markdown.echo.sync
import kotlin.test.Test
import kotlin.test.assertEquals

private sealed class GSetEvent<out T> {
  data class Add<out T>(val item: T) : GSetEvent<T>()
}

private fun <T> gSetProjection() =
    OneWayProjection<Set<T>, EventValue<GSetEvent<T>>> { event, model ->
      when (val body = event.value) {
        // Add events, and duplicate insertions.
        is GSetEvent.Add -> model + body.item
      }
    }

class GSetTest {

  @Test
  fun `one site can create a set and create new events`() = runBlocking {
    val alice = SiteIdentifier.random()
    val echo = mutableSite(
      identifier = alice,
      initial = emptySet<Int>(),
      projection = gSetProjection(),
    )

    echo.event {
      yield(GSetEvent.Add(1))
      yield(GSetEvent.Add(2))
      yield(GSetEvent.Add(3))
    }

    val result = echo.value.first { it.size == 3 }
    assertEquals(setOf(1, 2, 3), result)
  }

  @Test
  fun `two sites can create a shared set and eventually sync`() = runBlocking {
    // Create Alice, our first site.
    val alice = SiteIdentifier.random()
    val aliceEcho = mutableSite(
      identifier = alice,
      initial = emptySet<Int>(),
      projection = gSetProjection(),
    )

    // Create Bob, our second site.
    val bob = SiteIdentifier.random()
    val bobEcho = mutableSite(
      identifier = bob,
      initial = emptySet<Int>(),
      projection = gSetProjection(),
    )

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

    val aliceBeforeSync = aliceEcho.value.first { it.size == 2 }
    val bobBeforeSync = bobEcho.value.first { it.size == 3 }

    // Before sync, both sites have not merged their operations yet.
    assertEquals(setOf(1, 2), aliceBeforeSync)
    assertEquals(setOf(2, 3, 4), bobBeforeSync)

    // Sync both sites (with a timeout, since by default they'll keep the connection open until
    // either side cancels).
    try {
      withTimeout(timeMillis = 1000) { sync(aliceEcho, bobEcho) }
    } catch (expect: TimeoutCancellationException) {}

    // Finally, look at the resulting set of both sites.
    val aliceAfterSync = aliceEcho.value.first { it.size == 4 }
    val bobAfterSync = aliceEcho.value.first { it.size == 4 }

    // All the events are properly synced.
    val expected = setOf(1, 2, 3, 4)
    assertEquals(expected, aliceAfterSync)
    assertEquals(expected, bobAfterSync)
  }
}
