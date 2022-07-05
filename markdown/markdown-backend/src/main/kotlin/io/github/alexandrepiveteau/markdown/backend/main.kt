@file:JvmName("Main")

package io.github.alexandrepiveteau.markdown.backend

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.websocket.*

/**
 * Returns the port number to use when running the web application. Defaults to 1234 if no port is
 * specified.
 */
private val Port = System.getenv("PORT")?.toIntOrNull() ?: 1234

fun main() {
  val groups = GroupMap()
  val server =
      embeddedServer(CIO, port = Port) {
        install(WebSockets)
        signaling(groups)
      }
  server.start(wait = true)
}
