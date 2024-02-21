package io.github.alexandrepiveteau.echo.webrtc.server

import io.github.alexandrepiveteau.echo.DefaultBinaryFormat
import io.github.alexandrepiveteau.echo.webrtc.server.groups.GroupMap
import io.github.alexandrepiveteau.echo.webrtc.server.groups.session
import io.github.alexandrepiveteau.echo.webrtc.signaling.SignalingMessage.ClientToServer
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.decodeFromByteArray

/**
 * Handles the signaling of webRTC sessions.
 *
 * @receiver the [Route] at which the endpoint is created.
 * @param scope the [CoroutineScope] in which the sessions should be kept.
 * @param session a function which maps the [ApplicationCall] to its associated [SessionIdentifier].
 */
public fun Route.signaling(
    scope: CoroutineScope,
    session: (ApplicationCall) -> SessionIdentifier,
) {
  val groups = GroupMap(scope, application.log)
  webSocket {
    groups.get(session(call)).session(this) { myId ->
      for (frame in incoming) {
        val bytes = (frame as Frame.Binary).readBytes()
        val msg = DefaultBinaryFormat.decodeFromByteArray<ClientToServer>(bytes)
        forward(msg.to, msg.toServerToClient(myId))
      }
    }
  }
}
