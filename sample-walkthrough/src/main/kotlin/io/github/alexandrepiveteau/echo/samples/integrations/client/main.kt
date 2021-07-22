package io.github.alexandrepiveteau.echo.samples.integrations.client

import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.ktor.wsExchange
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.samples.basics.c.GSet
import io.github.alexandrepiveteau.echo.serialization.encodeToFrame
import io.github.alexandrepiveteau.echo.sync
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

suspend fun main() = coroutineScope {
  val client = HttpClient(CIO) { install(WebSockets) }

  val remote =
      client.wsExchange(
          receiver = {
            url {
              path("/rcv")
              host = "localhost"
              port = 1234
            }
          },
          sender = {
            url {
              path("/snd")
              host = "localhost"
              port = 1234
            }
          },
      )

  val local = mutableSite(Random.nextSiteIdentifier(), initial = emptySet(), projection = GSet)

  val syncJob = launch { sync(local.encodeToFrame(), remote) }
  val updateJob = launch { local.value.collect { println("Local value is $it") } }

  while (true) {
    when (readLine()) {
      "exit" -> break
      "emit" -> local.event { yield(Random.nextInt(0, 100)) }
    }
  }

  syncJob.cancel()
  updateJob.cancel()
}
