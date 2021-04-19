@file:JvmName("Main")

package io.github.alexandrepiveteau.markdown.backend

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.ktor.server.EchoKtorServerPreview
import io.github.alexandrepiveteau.echo.ktor.server.receiver
import io.github.alexandrepiveteau.echo.ktor.server.sender
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.protocol.encode
import io.github.alexandrepiveteau.markdown.ServerReceiverPath
import io.github.alexandrepiveteau.markdown.ServerSenderPath
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import party.markdown.MarkdownEvent

@OptIn(EchoKtorServerPreview::class)
fun main() {
  val site = mutableSite<MarkdownEvent>(SiteIdentifier.random())
  val server =
      embeddedServer(CIO, port = 8080) {
        install(WebSockets)
        routing {
          route("/$ServerSenderPath") { sender(site.encode(MarkdownEvent)) }
          route("/$ServerReceiverPath") { receiver(site.encode(MarkdownEvent)) }
        }
      }
  server.start(wait = true)
}
