@file:JvmName("Main")

package party.markdown

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.exchange
import io.github.alexandrepiveteau.echo.ktor.server.receiver
import io.github.alexandrepiveteau.echo.ktor.server.sender
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.github.alexandrepiveteau.echo.serialization.encodeToFrame
import io.github.alexandrepiveteau.echo.webrtc.server.SessionIdentifier
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

/** The [Mutex] to guarantee exclusive access to each session document. */
private val Mutex = Mutex()

/** The sessions that are currently running. */
private val Sessions = mutableMapOf<SessionIdentifier, Exchange<Incoming, Outgoing>>()

/**
 * Returns the session associated with the given identifier.
 *
 * @param id the identifier for the session.
 * @return the corresponding [Exchange].
 */
private suspend fun session(id: SessionIdentifier): Exchange<Frame, Frame> {
  val session = Mutex.withLock { Sessions.getOrPut(id) { exchange(MarkdownPartyHistory()) } }
  return session.encodeToFrame()
}

suspend fun main(): Unit = coroutineScope {
  val server =
      embeddedServer(CIO, port = Port) {
        install(WebSockets)
        routing {
          route("groups/{$IdArgument}") {
            route("snd") { sender { session(call.requireId()) } }
            route("rcv") { receiver { session(call.requireId()) } }
          }
        }
      }
  server.start(wait = true)
}
