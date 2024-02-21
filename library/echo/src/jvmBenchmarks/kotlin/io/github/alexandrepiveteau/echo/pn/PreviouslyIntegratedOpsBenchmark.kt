package io.github.alexandrepiveteau.echo.pn

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toUInt
import io.github.alexandrepiveteau.echo.core.log.mutableHistoryOf
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.projections.ChangeScope
import io.github.alexandrepiveteau.echo.projections.TwoWayMutableProjection
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection
import io.github.alexandrepiveteau.echo.sync
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

@Serializable
private enum class Example {
  Increment
}

private fun unit(): Op = Example.Increment

private typealias Op = Example

@State(Scope.Thread)
class PreviouslyIntegratedOpsBenchmark {

  @Param("2", "3", "4", "5", "6", "7", "8") var replicas = 0
  @Param("0", "100", "1000", "10000") var previouslyIntegrated = 0
  @Param("1000") var increments = 0

  private object Projection : TwoWayProjection<Int, Op, Op> {

    override fun ChangeScope<Op>.forward(
        model: Int,
        id: EventIdentifier,
        event: Op,
    ): Int {
      push(event)
      return model + 1
    }

    override fun backward(
        model: Int,
        id: EventIdentifier,
        event: Op,
        change: Op,
    ): Int {
      return model - 1
    }
  }

  @Benchmark
  fun previouslyIntegrated() = runBlocking {
    val primary = mutableSite(SiteIdentifier.Max, 0, Projection)
    repeat(previouslyIntegrated) { primary.event { yield(unit()) } }

    repeat(replicas) {
      val id = (SiteIdentifier.Min.toUInt() + it.toUInt()).toSiteIdentifier()
      val replica = mutableSite(id, 0, Projection)
      val syncJob = launch { sync(replica, primary) }

      launch {
        // First, await that the counter reaches at least the initial value (await the sync).
        replica.value.first { count -> count >= previouslyIntegrated }

        // Then, perform the increments. Await that the total reaches the right value.
        repeat(increments) { replica.event { yield(unit()) } }
        replica.value.first { count -> count == previouslyIntegrated + increments * replicas }
        syncJob.cancel()
      }
    }
  }
}
