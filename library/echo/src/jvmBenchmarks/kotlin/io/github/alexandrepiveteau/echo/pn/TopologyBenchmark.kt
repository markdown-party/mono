package io.github.alexandrepiveteau.echo.pn

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toUInt
import io.github.alexandrepiveteau.echo.core.log.mutableEventLogOf
import io.github.alexandrepiveteau.echo.core.log.mutableTreeEventLogOf
import io.github.alexandrepiveteau.echo.exchange
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.pn.TopologyBenchmark.LogType.GapBuffer
import io.github.alexandrepiveteau.echo.pn.TopologyBenchmark.LogType.Tree
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import io.github.alexandrepiveteau.echo.sync
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@State(Scope.Thread)
class TopologyBenchmark {

  @Param("2", "3", "4", "5", "6", "7", "8") var replicas = 0
  @Param("GapBuffer") var mode = GapBuffer
  @Param("500") var increments = 0

  enum class LogType {
    GapBuffer,
    Tree
  }

  /**
   * Returns an [Exchange] with the proper [EventLog] implementation.
   *
   * @param type the [LogType] to use.
   */
  private fun primary(type: LogType): Exchange<Inc, Out> {
    val eventLog =
        when (type) {
          GapBuffer -> mutableEventLogOf()
          Tree -> mutableTreeEventLogOf()
        }
    return exchange(eventLog)
  }

  @Benchmark
  fun twoWay() = runBlocking {
    val primary = primary(mode)
    repeat(replicas) {
      val id = (SiteIdentifier.Min.toUInt() + it.toUInt()).toSiteIdentifier()
      val replica = mutableSite(identifier = id, initial = 0, projection = TwoWayPNCounter)
      val syncJob = launch { sync(replica, primary) }

      launch {
        repeat(increments) { replica.event { yield(1) } }
        replica.value.first { c -> c == replicas * increments }
        syncJob.cancel()
      }
    }
  }

  @Benchmark
  fun oneWay() = runBlocking {
    val primary = primary(mode)
    repeat(replicas) {
      val id = (SiteIdentifier.Min.toUInt() + it.toUInt()).toSiteIdentifier()
      val replica =
          mutableSite(identifier = id, initial = persistentMapOf(), projection = OneWayPNCounter)
      val syncJob = launch { sync(replica, primary) }

      launch {
        repeat(increments) { count -> replica.event { yield(count) } }
        replica.value.first { c -> c.values.sum() == replicas * (increments - 1) }
        syncJob.cancel()
      }
    }
  }

  @Benchmark
  fun optimized() = runBlocking {
    val primary = primary(mode)
    repeat(replicas) {
      val id = (SiteIdentifier.Min.toUInt() + it.toUInt()).toSiteIdentifier()
      val replica = fastCounter(identifier = id)
      val syncJob = launch { sync(replica, primary) }

      launch {
        repeat(increments) { count -> replica.event { yield(count) } }
        replica.value.first { sum -> sum == replicas * (increments - 1) }
        syncJob.cancel()
      }
    }
  }
}
