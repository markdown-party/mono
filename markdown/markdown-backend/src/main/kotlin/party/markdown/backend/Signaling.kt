package party.markdown.backend

import io.github.alexandrepiveteau.echo.DefaultBinaryFormat
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromByteArray
import party.markdown.backend.groups.GroupMap
import party.markdown.backend.groups.session
import party.markdown.signaling.SignalingMessage.ClientToServer

/** The value of the id argument. */
private const val IdArgument = "id"

/** Retrieves the identifier of the collaboration group. */
private fun WebSocketServerSession.requireId(): SessionIdentifier =
    SessionIdentifier(requireNotNull(call.parameters[IdArgument]))

/**
 * Handles the signaling of webRTC sessions.
 *
 * @param groups the [GroupMap] which keeps track of all the current groups.
 */
fun Application.signaling(groups: GroupMap) {
  routing {
    route("groups/{$IdArgument}") {
      webSocket {
        groups.get(requireId()).session(this) { myId ->
          for (frame in incoming) {
            val bytes = (frame as Frame.Binary).readBytes()
            val msg = DefaultBinaryFormat.decodeFromByteArray<ClientToServer>(bytes)
            forward(msg.to, msg.toServerToClient(myId))
          }
        }
      }
    }
  }
}
