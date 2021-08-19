package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.protocol.Message
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.ktor.http.cio.websocket.*
import io.ktor.http.cio.websocket.Frame.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer

/**
 * Encodes an [Exchange] of [Message] into an [Exchange] of [Frame], using the default event
 * serializer. The frames can then directly be sent over websockets.
 *
 * @return the encoded [Exchange]
 */
fun Exchange<Incoming, Outgoing>.encodeToFrame(): Exchange<Frame, Frame> =
    object : Exchange<Frame, Frame> {

      override fun receive(
          incoming: Flow<Frame>,
      ) = transformFromFrame(incoming, this@encodeToFrame::receive)

      override fun send(
          incoming: Flow<Frame>,
      ) = transformFromFrame(incoming, this@encodeToFrame::send)
    }

/**
 * Decodes an [Exchange] of [Frame] into an [Exchange] of [Message], using the default event
 * serializer.
 *
 * @return the decoded [Exchange]
 */
fun Exchange<Frame, Frame>.decodeFromFrame(): Exchange<Incoming, Outgoing> =
    object : Exchange<Incoming, Outgoing> {

      override fun receive(
          incoming: Flow<Outgoing>,
      ): Flow<Incoming> = transformToFrame(incoming, this@decodeFromFrame::receive)

      override fun send(
          incoming: Flow<Incoming>,
      ): Flow<Outgoing> = transformToFrame(incoming, this@decodeFromFrame::send)
    }

// FLOW ENCODING AND DECODING

private inline fun <reified A, reified B> transformFromFrame(
    incoming: Flow<Frame>,
    f: (Flow<A>) -> Flow<B>,
): Flow<Frame> {
  val incomingBytes = incoming.filterIsInstance<Binary>().map(Binary::readBytes)
  val transformed = f(incomingBytes.decode())
  return transformed.encode().map { Binary(true, it) }
}

private inline fun <reified A, reified B> transformToFrame(
    incoming: Flow<A>,
    f: (Flow<Frame>) -> Flow<Frame>
): Flow<B> {
  val incomingFrames = incoming.encode().map { Binary(true, it) }
  val transformed = f(incomingFrames)
  return transformed.filterIsInstance<Binary>().map(Binary::readBytes).decode()
}

private inline fun <reified T> Flow<ByteArray>.decode(
    serializer: KSerializer<T> = serializer(),
) = map { msg -> ProtoBuf.decodeFromByteArray(serializer, msg) }

private inline fun <reified T> Flow<T>.encode(
    serializer: KSerializer<T> = serializer(),
) = map { msg -> ProtoBuf.encodeToByteArray(serializer, msg) }
