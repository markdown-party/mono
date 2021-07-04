package io.github.alexandrepiveteau.echo

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.test.Ignore

class ScalabilityTest {

  /**
   * A [OneWayProjection] that simply takes the last value. The operation is commutative, idempotent
   * and associative.
   */
  private class LWWProjection<T> : OneWayProjection<Pair<EventIdentifier, T>, T> {

    override fun forward(
        model: Pair<EventIdentifier, T>,
        identifier: EventIdentifier,
        event: T
    ): Pair<EventIdentifier, T> {
      val (id, data) = model
      return if (identifier >= id) identifier to event else id to data
    }
  }

  // TODO : See how we may discard irrelevant operations.

  @Ignore
  @Test
  fun testScalability() = suspendTest {
    val sites = 100
    val ops = 1000
    val projection = LWWProjection<Int>()
    val initial = EventIdentifier(SequenceNumber.Min, SiteIdentifier.Min) to 0

    // Measured performance - Standard log (immutable) : up to         49'999 op-site/sec.
    // Measured performance - Custom log   (immutable) : more than  1'000'000 op-site/sec.
    //
    // Of course, the huge performance boost comes from skipped operations from the primary, which
    // does not have to deliver intermediate log values to the replica.

    val millis = measureTimeMillis {
      withContext(Dispatchers.Default) {
        val primary = mutableSite(Random.nextSiteIdentifier(), initial, projection = projection)
        val done = Channel<SiteIdentifier>()

        repeat(sites) {
          val id = Random.nextSiteIdentifier()
          val replica = mutableSite(id, initial, projection = projection)

          val syncJob = launch { sync(replica, primary) }
          launch {
            replica.value.first { (_, it) -> it == 1001 }
            syncJob.cancel()
            done.send(id)
          }
        }

        repeat(ops) { primary.event { yield(1000) } }
        primary.event { yield(1001) }

        // We're done replicating the events everywhere.
        repeat(sites) { done.receive() }
      }
    }

    val speed = (sites * ops * 1000.0) / (millis)
    println("Took $millis ms, at ${speed.roundToInt()} ops-site/sec.")
  }
}
