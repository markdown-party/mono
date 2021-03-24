package markdown.echo.demo.counter

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import markdown.echo.demo.Site
import markdown.echo.demo.counter.PNCounterEvent.Decrement
import markdown.echo.demo.counter.PNCounterEvent.Increment
import markdown.echo.events.event
import markdown.echo.projections.OneWayProjection
import markdown.echo.projections.projection
import markdown.echo.sync
import kotlin.test.Test

private sealed class PNCounterEvent {
  object Increment : PNCounterEvent() {
    override fun toString(): String = "PNCounterEvent.Increment"
  }
  object Decrement : PNCounterEvent() {
    override fun toString(): String = "PNCounterEvent.Decrement"
  }
}

private val PNProjection =
    OneWayProjection<Int, PNCounterEvent> { event, sum ->
      when (event) {
        is Increment -> sum + 1
        is Decrement -> sum - 1
      }
    }

class PNCounterTest {

  @Test
  fun `two sites can create a shared counter and eventually sync`(): Unit = runBlocking {
    val (alice, bob) = Site.createMemoryEchos<PNCounterEvent>()

    alice.event {
      yield(Decrement)
      yield(Increment)
      yield(Decrement)
    }

    bob.event {
      yield(Increment)
      yield(Increment)
    }

    // Sync both sites (with a timeout, since by default they'll keep the connection open until
    // either side cancels).
    try {
      withTimeout(timeMillis = 1000) { sync(alice, bob) }
    } catch (expect: TimeoutCancellationException) {}

    // Finally, look at the resulting set of both sites, and make sure they eventually reach the
    // right result.
    alice.projection(0, PNProjection).first { it == 1 }
    bob.projection(0, PNProjection).first { it == 1 }
  }
}
