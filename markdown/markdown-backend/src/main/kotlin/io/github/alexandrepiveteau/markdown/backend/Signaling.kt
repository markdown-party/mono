package io.github.alexandrepiveteau.markdown.backend

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

/** The value of the id argument. */
private const val IdArgument = "id"

/** Retrieves the identifier of the collaboration group. */
private fun WebSocketServerSession.requireId(): String = requireNotNull(call.parameters[IdArgument])

/**
 * Handles the signaling of webRTC sessions.
 *
 * @param groups the [GroupMap] which keeps track of all the current groups.
 */
fun Application.signaling(groups: GroupMap) {
  routing {
    route("groups/{$IdArgument}") {
      webSocket {
        groups.get(requireId()).session(this) {
          for (msg in incoming) {
            msg as Frame.Text
            msg.readText()
          }
        }
      }
    }
  }
}
