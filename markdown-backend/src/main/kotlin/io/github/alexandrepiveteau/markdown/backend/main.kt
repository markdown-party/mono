package io.github.alexandrepiveteau.markdown.backend

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.ktor.server.EchoKtorServerPreview
import io.github.alexandrepiveteau.echo.ktor.server.receiver
import io.github.alexandrepiveteau.echo.ktor.server.sender
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.protocol.encode
import io.github.alexandrepiveteau.markdown.Coder
import io.github.alexandrepiveteau.markdown.CounterEvent
import io.github.alexandrepiveteau.markdown.ServerReceiverPath
import io.github.alexandrepiveteau.markdown.ServerSenderPath
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*

@OptIn(EchoKtorServerPreview::class)
fun main() {
  val site = mutableSite<CounterEvent>(SiteIdentifier.random())
  val server =
      embeddedServer(Netty, port = 8080) {
        install(WebSockets)
        routing {
          route("/hello") { get { call.respondText("Hello world.") } }
          route("/$ServerSenderPath") { sender(site.encode(Coder)) }
          route("/$ServerReceiverPath") { receiver(site.encode(Coder)) }
        }
      }
  server.start(wait = true)
}
