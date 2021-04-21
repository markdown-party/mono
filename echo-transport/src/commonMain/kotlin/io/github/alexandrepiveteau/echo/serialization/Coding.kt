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

/**
 * Encodes an [Exchange] of [Message] into an [Exchange] of [Frame], using the provided event
 * serializer. The frames can then directly be sent over websockets.
 *
 * @param serializer the [KSerializer] used to perform event mappings.
 * @param T the type of the events of this [Exchange].
 *
 * @return the encoded [Exchange]
 */
fun <T> Exchange<Message.Incoming<T>, Message.Outgoing<T>>.encodeToFrame(
    serializer: KSerializer<T>,
): Exchange<Frame, Frame> =
    object : Exchange<Frame, Frame> {
      override fun outgoing() =
          Link<Frame, Frame> { inc ->
            this@encodeToFrame.encodeToString(serializer)
                .outgoing()
                .talk(inc.filterIsInstance<Frame.Text>().map(Frame.Text::readText))
                .map(Frame::Text)
          }

      override fun incoming() =
          Link<Frame, Frame> { inc ->
            this@encodeToFrame.encodeToString(serializer)
                .incoming()
                .talk(inc.filterIsInstance<Frame.Text>().map(Frame.Text::readText))
                .map(Frame::Text)
          }
    }

/**
 * Decodes an [Exchange] of [Frame] into an [Exchange] of [Message], using the provided event
 * serializer.
 *
 * @param serializer the [KSerializer] used to perform event mappings.
 * @param T the type of the events of this [Exchange].
 *
 * @return the decoded [Exchange]
 */
fun <T> Exchange<Frame, Frame>.decodeFromFrame(
    serializer: KSerializer<T>,
): Exchange<Message.Incoming<T>, Message.Outgoing<T>> =
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
        .decodeFromString(serializer)

// STRING JSON CODING

internal fun <T> Exchange<String, String>.decodeFromString(
    elementSerializer: KSerializer<T>,
): Exchange<Message.Incoming<T>, Message.Outgoing<T>> =
    object : Exchange<Message.Incoming<T>, Message.Outgoing<T>> {

      override fun outgoing() =
          this@decodeFromString.outgoing()
              .decodeFromString(
                  Message.Incoming.serializer(elementSerializer),
                  Message.Outgoing.serializer<T>(),
              )

      override fun incoming() =
          this@decodeFromString.incoming()
              .decodeFromString(
                  Message.Outgoing.serializer<T>(),
                  Message.Incoming.serializer(elementSerializer),
              )
    }

internal fun <T> Exchange<Message.Incoming<T>, Message.Outgoing<T>>.encodeToString(
    elementSerializer: KSerializer<T>,
): Exchange<String, String> =
    object : Exchange<String, String> {

      override fun outgoing() =
          this@encodeToString.outgoing()
              .encodeToString(
                  Message.Incoming.serializer(elementSerializer),
                  Message.Outgoing.serializer(),
              )

      override fun incoming() =
          this@encodeToString.incoming()
              .encodeToString(
                  Message.Outgoing.serializer(),
                  Message.Incoming.serializer(elementSerializer),
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
