@file:JvmName("Main")

package io.github.alexandrepiveteau.markdown.backend

import io.github.alexandrepiveteau.echo.ktor.server.receiver
import io.github.alexandrepiveteau.echo.ktor.server.sender
import io.github.alexandrepiveteau.echo.serialization.encodeToFrame
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.websocket.*

/**
 * Returns the port number to use when running the web application. Defaults to 1234 if no port is
 * specified.
 */
private val Port = System.getenv("PORT")?.toIntOrNull() ?: 1234

/** Returns the [SessionIdentifier] associated with the current [WebSocketServerSession]. */
private fun WebSocketServerSession.requireSession(): SessionIdentifier {
  return call.parameters["session"] ?: error("No session provided.")
}

fun main() {
  val sites = SiteMap()
  val server =
      embeddedServer(CIO, port = Port) {
        install(WebSockets)
        routing {
          route("v1/{session}/snd") { sender { sites.get(requireSession()).encodeToFrame() } }
          route("v1/{session}/rcv") { receiver { sites.get(requireSession()).encodeToFrame() } }
        }
      }
  server.start(wait = true)
}
