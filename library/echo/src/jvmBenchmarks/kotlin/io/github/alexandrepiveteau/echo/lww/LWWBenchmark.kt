package io.github.alexandrepiveteau.echo.lww

import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.lww.LWWRegister.Tagged
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.sync
import io.github.alexandrepiveteau.echo.sync.SyncStrategy
import kotlin.random.Random
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@State(Scope.Thread)
class LWWBenchmark {

  @Param("2", "5", "10", "100") var participants = 0
  @Param("1", "10", "100", "10000", "1000000") var events = 0

  private fun site(identifier: SiteIdentifier) =
      mutableSite<Tagged<Int>, Int>(
          identifier = identifier,
          history = LWWMutableHistory(0),
          strategy = SyncStrategy.Once,
      )

  @Benchmark
  fun lww() = runBlocking {
    val primary = site(Random.nextSiteIdentifier())
    repeat(participants) {
      val id = Random.nextSiteIdentifier()
      val replica = site(id)
      launch {
        repeat(events) { n -> replica.event { yield(n) } }
        sync(primary, replica)
      }
    }
  }
}
