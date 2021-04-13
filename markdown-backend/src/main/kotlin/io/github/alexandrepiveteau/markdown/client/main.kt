package io.github.alexandrepiveteau.markdown.client

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.ktor.exchange
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.protocol.decode
import io.github.alexandrepiveteau.echo.sync
import io.github.alexandrepiveteau.markdown.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

suspend fun main(): Unit = coroutineScope {
  val client = HttpClient(CIO) { install(WebSockets) }
  val remote =
      client
          .exchange(
              receiver = {
                port = ServerPort
                url { path(ServerReceiverPath) }
              },
              sender = {
                port = 8080
                url { path(ServerSenderPath) }
              },
          )
          .decode(Coder)

  val alice =
      mutableSite<Int, CounterEvent>(
          identifier = SiteIdentifier.random(),
          initial = 0,
      ) { event, agg -> event.body.plus + agg }

  launch {
    try {
      sync(alice, remote)
    } catch (error: Throwable) {
      error.printStackTrace()
    }
  }
  launch { alice.value.collect { println("Got value $it") } }

  while (true) {
    println("Increment the counter by :")
    val number = readLine()?.toIntOrNull() ?: continue
    alice.event { yield(CounterEvent(plus = number)) }
  }
}
