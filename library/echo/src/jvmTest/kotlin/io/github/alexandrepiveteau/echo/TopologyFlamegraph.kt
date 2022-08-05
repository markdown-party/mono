package io.github.alexandrepiveteau.echo

import io.github.alexandrepiveteau.echo.TopologyFlamegraph.LogType.GapBuffer
import io.github.alexandrepiveteau.echo.TopologyFlamegraph.LogType.Tree
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toUInt
import io.github.alexandrepiveteau.echo.core.log.mutableEventLogOf
import io.github.alexandrepiveteau.echo.core.log.mutableTreeEventLogOf
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import kotlin.test.Test
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TopologyFlamegraph {

  var replicas = 4
  var mode = Tree
  var increments = 500

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

  @Test
  fun star_twoWay() = runBlocking {
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

  @Test
  fun star_oneWay() = runBlocking {
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
}
