package ktor

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

/**
 * An interface representing a subset of the features of [DefaultWebSocketSession]. Instead, it
 * offers some slightly different semantics for a websocket connection, where the outgoing buffer is
 * unlimited.
 */
interface BufferedWebSocketSession : CoroutineScope {

  /** @see DefaultWebSocketSession.incoming */
  val incoming: ReceiveChannel<Frame>

  /** @see DefaultWebSocketSession.outgoing */
  val outgoing: SendChannel<Frame>
}

/**
 * A variation of [HttpClient.ws] with a [BufferedWebSocketSession].
 *
 * @param request the block to create the HTTP request.
 * @param block the block executed in the websocket session.
 *
 * @see HttpClient.ws
 */
suspend fun HttpClient.bufferedWs(
    request: HttpRequestBuilder.() -> Unit,
    block: suspend BufferedWebSocketSession.() -> Unit
) = ws(request) { buffered(block)() }

/**
 * A variation of [HttpClient.wss] with a [BufferedWebSocketSession].
 *
 * @param request the block to create the HTTP request.
 * @param block the block executed in the websocket session.
 */
suspend fun HttpClient.bufferedWss(
    request: HttpRequestBuilder.() -> Unit,
    block: suspend BufferedWebSocketSession.() -> Unit,
) = wss(request) { buffered(block)() }

private fun buffered(
    block: suspend BufferedWebSocketSession.() -> Unit,
): suspend DefaultWebSocketSession.() -> Unit = {
  val buffer = Channel<Frame>(Channel.UNLIMITED)
  launch {
    for (frame in buffer) {
      outgoing.send(frame)
    }
    outgoing.close()
  }
  block(ActualBufferedWebSocketSession(this, buffer))
}

private class ActualBufferedWebSocketSession(
    session: DefaultWebSocketSession,
    buffer: Channel<Frame>,
) : BufferedWebSocketSession, CoroutineScope by session {
  override val incoming = session.incoming
  override val outgoing = buffer
}
