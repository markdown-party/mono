package markdown.party.demo.counter

import kotlin.test.Test
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import markdown.echo.logs.EventValue
import markdown.echo.projections.OneWayProjection
import markdown.echo.sync
import markdown.party.demo.Site
import markdown.party.demo.counter.PNCounterEvent.Decrement
import markdown.party.demo.counter.PNCounterEvent.Increment

private sealed class PNCounterEvent {
  object Increment : PNCounterEvent() {
    override fun toString(): String = "PNCounterEvent.Increment"
  }
  object Decrement : PNCounterEvent() {
    override fun toString(): String = "PNCounterEvent.Decrement"
  }
}

private val PNProjection =
    OneWayProjection<Int, EventValue<PNCounterEvent>> { event, sum ->
      when (event.value) {
        is Increment -> sum + 1
        is Decrement -> sum - 1
      }
    }

class PNCounterTest {

  @Test
  fun `two sites can create a shared counter and eventually sync`(): Unit = runBlocking {
    val (alice, bob) = Site.createMemoryEchos(0, PNProjection)

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
    alice.value.first { it == 1 }
    bob.value.first { it == 1 }
  }
}
