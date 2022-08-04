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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@State(Scope.Thread)
class TopologyBenchmark {

  @Param("2", "5", "10") var replicas = 0
  @Param("1", "10", "100", "1000") var increments = 0
  @Param("GapBuffer", "Tree") var mode = GapBuffer

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
  fun star() = runBlocking {
    val primary = primary(mode)
    repeat(replicas) {
      val id = (SiteIdentifier.Min.toUInt() + it.toUInt()).toSiteIdentifier()
      val replica = mutableSite(identifier = id, initial = 0, projection = PNCounter)
      val syncJob = launch { sync(replica, primary) }

      launch {
        repeat(increments) { replica.event { yield(1) } }
        replica.value.first { c -> c == replicas * increments }
        syncJob.cancel()
      }
    }
  }
}
