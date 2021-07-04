package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.Link
import io.github.alexandrepiveteau.echo.protocol.Message
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

// TODO : Use binary serialization instead of text-based serialization, since the messages now
//        support it out-of-the-box.

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
            this@encodeToFrame.encodeToString()
                .outgoing()
                .talk(inc.filterIsInstance<Frame.Text>().map(Frame.Text::readText))
                .map(Frame::Text)
          }

      override fun incoming() =
          Link<Frame, Frame> { inc ->
            this@encodeToFrame.encodeToString()
                .incoming()
                .talk(inc.filterIsInstance<Frame.Text>().map(Frame.Text::readText))
                .map(Frame::Text)
          }
    }

/**
 * Decodes an [Exchange] of [Frame] into an [Exchange] of [Message], using the default event
 * serializer.
 *
 * @return the decoded [Exchange]
 */
fun Exchange<Frame, Frame>.decodeFromFrame(): Exchange<Message.Incoming, Message.Outgoing> =
    object : Exchange<String, String> {

          override fun outgoing() =
              Link<String, String> { inc ->
                this@decodeFromFrame.outgoing()
                    .talk(inc.map(Frame::Text))
                    .filterIsInstance<Frame.Text>()
                    .map(Frame.Text::readText)
              }

          override fun incoming() =
              Link<String, String> { inc ->
                this@decodeFromFrame.incoming()
                    .talk(inc.map(Frame::Text))
                    .filterIsInstance<Frame.Text>()
                    .map(Frame.Text::readText)
              }
        }
        .decodeFromString()

// STRING JSON CODING

internal fun Exchange<String, String>.decodeFromString():
    Exchange<Message.Incoming, Message.Outgoing> =
    object : Exchange<Message.Incoming, Message.Outgoing> {

      override fun outgoing() =
          this@decodeFromString.outgoing()
              .decodeFromString(
                  Message.Incoming.serializer(),
                  Message.Outgoing.serializer(),
              )

      override fun incoming() =
          this@decodeFromString.incoming()
              .decodeFromString(
                  Message.Outgoing.serializer(),
                  Message.Incoming.serializer(),
              )
    }

internal fun Exchange<Message.Incoming, Message.Outgoing>.encodeToString():
    Exchange<String, String> =
    object : Exchange<String, String> {

      override fun outgoing() =
          this@encodeToString.outgoing()
              .encodeToString(
                  Message.Incoming.serializer(),
                  Message.Outgoing.serializer(),
              )

      override fun incoming() =
          this@encodeToString.incoming()
              .encodeToString(
                  Message.Outgoing.serializer(),
                  Message.Incoming.serializer(),
              )
    }

// FLOW ENCODING AND DECODING

private fun <T> Flow<String>.decode(
    serializer: KSerializer<T>,
) = map { msg -> Json.decodeFromString(serializer, msg) }

private fun <T> Flow<T>.encode(
    serializer: KSerializer<T>,
) = map { msg -> Json.encodeToString(serializer, msg) }

// LINK ENCODING AND DECODING

private fun <I, O> Link<String, String>.decodeFromString(
    a: KSerializer<I>,
    b: KSerializer<O>,
) = Link<I, O> { inc -> talk(inc.encode(a)).decode(b) }

private fun <I, O> Link<I, O>.encodeToString(
    a: KSerializer<I>,
    b: KSerializer<O>,
) = Link<String, String> { inc -> talk(inc.decode(a)).encode(b) }
