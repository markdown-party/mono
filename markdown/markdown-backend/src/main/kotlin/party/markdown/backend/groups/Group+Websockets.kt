package party.markdown.backend.groups

import io.github.alexandrepiveteau.echo.DefaultBinaryFormat
import io.github.alexandrepiveteau.echo.webrtc.signaling.PeerIdentifier
import io.ktor.websocket.*
import io.ktor.websocket.Frame.*
import kotlinx.serialization.encodeToByteArray

/**
 * Starts a session within this [Group], ensuring that the user properly joins and then leaves the
 * group.
 *
 * @receiver the [Group] in which the session takes place.
 * @param session the [WebSocketSession] used for communication.
 * @param block the [GroupScope] in which the participant may forward some messages.
 */
suspend fun Group.session(
    session: WebSocketSession,
    block: suspend GroupScope.(PeerIdentifier) -> Unit,
) =
    session(
        Outbox.wrap(session.outgoing).map {
          Binary(true, DefaultBinaryFormat.encodeToByteArray(it))
        },
        block,
    )
