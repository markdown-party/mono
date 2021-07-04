package io.github.alexandrepiveteau.echo.demo.counter

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.demo.counter.PNCounterEvent.Decrement
import io.github.alexandrepiveteau.echo.demo.counter.PNCounterEvent.Increment
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.projections.ChangeScope
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection
import io.github.alexandrepiveteau.echo.suspendTest
import io.github.alexandrepiveteau.echo.sync
import kotlin.test.Test
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable

@Serializable
private enum class PNCounterEvent {
  Increment,
  Decrement,
}

private object PNProjection : TwoWayProjection<Int, PNCounterEvent, PNCounterEvent> {

  override fun ChangeScope<PNCounterEvent>.forward(
      model: Int,
      id: EventIdentifier,
      event: PNCounterEvent
  ): Int =
      when (event) {
        Increment -> model + 1
        Decrement -> model - 1
      }.also { push(event) }

  override fun backward(
      model: Int,
      id: EventIdentifier,
      event: PNCounterEvent,
      change: PNCounterEvent
  ): Int =
      when (change) {
        Increment -> model - 1
        Decrement -> model + 1
      }
}

class PNCounterTest {

  @Test
  fun twoSitesCanCreateASharedCounter_andSync(): Unit = suspendTest {
    val alice = mutableSite(SiteIdentifier.Min, 0, PNProjection)
    val bob = mutableSite(SiteIdentifier.Max, 0, PNProjection)

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
