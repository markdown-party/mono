package io.github.alexandrepiveteau.echo.samples.drawing.backend

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.ktor.server.receiver
import io.github.alexandrepiveteau.echo.ktor.server.sender
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.protocol.encode
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.DrawingEvent
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*

fun main() {
  val site = mutableSite<DrawingEvent>(SiteIdentifier.random())
  val server =
      embeddedServer(Netty, port = 8080) {
        install(WebSockets)
        routing {
          route("/hello") { get { call.respondText("Hello world.") } }
          route("/sender") { sender(site.encode(DrawingEvent)) }
          route("/receiver") { receiver(site.encode(DrawingEvent)) }
        }
      }
  server.start(wait = true)
}
