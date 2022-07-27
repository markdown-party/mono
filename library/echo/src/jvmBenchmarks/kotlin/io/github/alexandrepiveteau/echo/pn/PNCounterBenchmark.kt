package io.github.alexandrepiveteau.echo.pn

import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.sync
import kotlin.random.Random
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@State(Scope.Thread)
class PNCounterBenchmark {

  @Param("2", "5", "10", "100") var participants = 0
  @Param("1", "10", "100", "10000") var increments = 0

  private fun site(identifier: SiteIdentifier) =
      mutableSite(identifier = identifier, initial = 0, projection = PNCounter)

  @Benchmark
  fun counter() = runBlocking {
    val primary = site(Random.nextSiteIdentifier())
    repeat(participants) {
      val id = Random.nextSiteIdentifier()
      val replica = site(id)
      val syncJob = launch { sync(replica, primary) }

      launch {
        repeat(increments) { replica.event { yield(1) } }
        replica.value.first { it == participants * increments }
        syncJob.cancel()
      }
    }
  }
}
