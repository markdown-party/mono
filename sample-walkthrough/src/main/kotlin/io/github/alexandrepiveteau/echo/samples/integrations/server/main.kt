package io.github.alexandrepiveteau.echo.samples.integrations.server

import io.github.alexandrepiveteau.echo.exchange
import io.github.alexandrepiveteau.echo.ktor.server.receiver
import io.github.alexandrepiveteau.echo.ktor.server.sender
import io.github.alexandrepiveteau.echo.serialization.encodeToFrame
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

fun main() {
  val site = exchange()
  embeddedServer(CIO, port = 1234) {
        install(WebSockets)
        routing {
          route("snd") { sender { site.encodeToFrame() } }
          route("rcv") { receiver { site.encodeToFrame() } }
        }
      }
      .start(wait = true)
}
