@file:JvmName("Main")

package party.markdown

import io.github.alexandrepiveteau.echo.webrtc.server.SessionIdentifier
import io.github.alexandrepiveteau.echo.webrtc.server.signaling
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.coroutineScope

/**
 * Returns the port number to use when running the web application. Defaults to 1234 if no port is
 * specified.
 */
private val Port = System.getenv("PORT")?.toIntOrNull() ?: 1234

/** The value of the id argument. */
private const val IdArgument = "id"

/** Retrieves the identifier of the collaboration group. */
private fun ApplicationCall.requireId(): SessionIdentifier =
    SessionIdentifier(requireNotNull(parameters[IdArgument]))

suspend fun main(): Unit = coroutineScope {
  val server =
      embeddedServer(CIO, port = Port) {
        install(WebSockets)
        routing {
          route("groups/{$IdArgument}") {
            signaling(this@coroutineScope, ApplicationCall::requireId)
          }
        }
      }
  server.start(wait = true)
}
