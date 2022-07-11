@file:JvmName("Main")

package party.markdown

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.coroutineScope
import party.markdown.backend.groups.GroupMap
import party.markdown.backend.signaling

/**
 * Returns the port number to use when running the web application. Defaults to 1234 if no port is
 * specified.
 */
private val Port = System.getenv("PORT")?.toIntOrNull() ?: 1234

suspend fun main(): Unit = coroutineScope {
  val groups = GroupMap(this)
  val server =
      embeddedServer(CIO, port = Port) {
        install(WebSockets)
        signaling(groups)
      }
  server.start(wait = true)
}
