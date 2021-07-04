package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.Link
import io.github.alexandrepiveteau.echo.protocol.Message
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Encodes an [Exchange] of [Message] into an [Exchange] of [Frame], using the default event
 * serializer. The frames can then directly be sent over websockets.
 *
 * @return the encoded [Exchange]
 */
fun Exchange<Message.Incoming, Message.Outgoing>.encodeToFrame(): Exchange<Frame, Frame> =
    object : Exchange<Frame, Frame> {
      override fun outgoing() =
          Link<Frame, Frame> { inc ->
            this@encodeToFrame.encodeToByteArray()
                .outgoing()
                .talk(inc.filterIsInstance<Frame.Binary>().map(Frame.Binary::readBytes))
                .map { Frame.Binary(true, it) }
          }

      override fun incoming() =
          Link<Frame, Frame> { inc ->
            this@encodeToFrame.encodeToByteArray()
                .incoming()
                .talk(inc.filterIsInstance<Frame.Binary>().map(Frame.Binary::readBytes))
                .map { Frame.Binary(true, it) }
          }
    }

/**
 * Decodes an [Exchange] of [Frame] into an [Exchange] of [Message], using the default event
 * serializer.
 *
 * @return the decoded [Exchange]
 */
fun Exchange<Frame, Frame>.decodeFromFrame(): Exchange<Message.Incoming, Message.Outgoing> =
    object : Exchange<ByteArray, ByteArray> {

          override fun outgoing() =
              Link<ByteArray, ByteArray> { inc ->
                this@decodeFromFrame.outgoing()
                    .talk(inc.map { Frame.Binary(true, it) })
                    .filterIsInstance<Frame.Binary>()
                    .map(Frame.Binary::readBytes)
              }

          override fun incoming() =
              Link<ByteArray, ByteArray> { inc ->
                this@decodeFromFrame.incoming()
                    .talk(inc.map { Frame.Binary(true, it) })
                    .filterIsInstance<Frame.Binary>()
                    .map(Frame.Binary::readBytes)
              }
        }
        .decodeFromByteArray()

// STRING JSON CODING

internal fun Exchange<ByteArray, ByteArray>.decodeFromByteArray():
    Exchange<Message.Incoming, Message.Outgoing> =
    object : Exchange<Message.Incoming, Message.Outgoing> {

      override fun outgoing() =
          this@decodeFromByteArray.outgoing()
              .decodeFromByteArray(
                  Message.Incoming.serializer(),
                  Message.Outgoing.serializer(),
              )

      override fun incoming() =
          this@decodeFromByteArray.incoming()
              .decodeFromByteArray(
                  Message.Outgoing.serializer(),
                  Message.Incoming.serializer(),
              )
    }

internal fun Exchange<Message.Incoming, Message.Outgoing>.encodeToByteArray():
    Exchange<ByteArray, ByteArray> =
    object : Exchange<ByteArray, ByteArray> {

      override fun outgoing() =
          this@encodeToByteArray.outgoing()
              .encodeToByteArray(
                  Message.Incoming.serializer(),
                  Message.Outgoing.serializer(),
              )

      override fun incoming() =
          this@encodeToByteArray.incoming()
              .encodeToByteArray(
                  Message.Outgoing.serializer(),
                  Message.Incoming.serializer(),
              )
    }

// FLOW ENCODING AND DECODING

private fun <T> Flow<ByteArray>.decode(
    serializer: KSerializer<T>,
) = map { msg -> ProtoBuf.decodeFromByteArray(serializer, msg) }

private fun <T> Flow<T>.encode(
    serializer: KSerializer<T>,
) = map { msg -> ProtoBuf.encodeToByteArray(serializer, msg) }

// LINK ENCODING AND DECODING

private fun <I, O> Link<ByteArray, ByteArray>.decodeFromByteArray(
    a: KSerializer<I>,
    b: KSerializer<O>,
) = Link<I, O> { inc -> talk(inc.encode(a)).decode(b) }

private fun <I, O> Link<I, O>.encodeToByteArray(
    a: KSerializer<I>,
    b: KSerializer<O>,
) = Link<ByteArray, ByteArray> { inc -> talk(inc.decode(a)).encode(b) }
