package io.github.alexandrepiveteau.echo

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier.Companion.random
import io.github.alexandrepiveteau.echo.logs.EventLog.IndexedEvent
import io.github.alexandrepiveteau.echo.logs.persistentEventLogOf
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Scalability {

  /** A [OneWayProjection] that simply takes the last value. */
  private class LWWProjection<T> : OneWayProjection<T, IndexedEvent<T>> {
    override fun forward(body: IndexedEvent<T>, model: T): T = body.body
  }

  @Test
  fun `test scalability`() = suspendTest {
    val sites = 100
    val ops = 1000
    val projection = LWWProjection<Int>()
    val log = persistentEventLogOf<Int>()

    // Measured performance - Standard log : up to      49'999 op-site/sec.
    // Measured performance - Custom log   : more than  50'000 op-site/sec.

    val millis = measureTimeMillis {
      withContext(Dispatchers.Default) {
        val primary = mutableSite(random(), 0, log = log, projection = projection)
        val done = Channel<SiteIdentifier>()

        repeat(sites) {
          val id = random()
          val replica = mutableSite(id, 0, log = log, projection = projection)

          val syncJob = launch { sync(replica, primary) }
          launch {
            replica.value.first { it == 1001 }
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
