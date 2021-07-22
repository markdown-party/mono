package io.github.alexandrepiveteau.echo

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.demo.counter.PNCounterEvent
import io.github.alexandrepiveteau.echo.demo.counter.PNProjection
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import io.github.alexandrepiveteau.echo.sync.SyncStrategy.Companion.Once
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.measureTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    val duration = measureTime {
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

    val speed = (sites * ops * 1000.0) / (duration.inWholeMilliseconds)
    println("Took ${duration.inWholeMilliseconds} ms, at ${speed.roundToInt()} ops-site/sec.")
  }

  @Ignore
  @Test
  fun compareMutableHistoryToExchange() = suspendTest {
    val ops = 100_000
    val primary = mutableSite(SiteIdentifier.Min, 0, PNProjection, strategy = Once)
    repeat(ops) { primary.event { yield(PNCounterEvent.Increment) } }

    val history = mutableSite(SiteIdentifier.Max, 0, PNProjection)
    val exchange = exchange()

    val historyDuration = measureTime { sync(primary, history) }
    val exchangeDuration = measureTime { sync(primary, exchange) }
    val speed =
        ((historyDuration.inWholeMilliseconds.toFloat() /
                exchangeDuration.inWholeMilliseconds.toFloat()) * 100)
            .roundToInt()
    val historyOps = ops * 1000.0 / historyDuration.inWholeMilliseconds
    val exchangeOps = ops * 1000.0 / exchangeDuration.inWholeMilliseconds

    // Measured performance (history)  :       25'000 ops/sec
    // Measured performance (exchange) :       35'000 ops/sec (125% - 150%)
    println("Took ${historyDuration.inWholeMilliseconds} ms for history, at $historyOps ops/sec.")
    println("Took ${exchangeDuration.inWholeMilliseconds} ms for exchange (speed ${speed}%), at $exchangeOps ops/sec.")
  }
}
