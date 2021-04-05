package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.Coder
import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.coding
import io.github.alexandrepiveteau.echo.reversed

/**
 * Returns a [Coder] for [Message.V1.Incoming] and [Transport.V1.Incoming].
 *
 * @param T the type of the event body.
 */
fun <T> Message.V1.Incoming.Companion.coder() =
    object : Coder<Message.V1.Incoming<T>, Transport.V1.Incoming<T>> {
      override suspend fun encode(it: Message.V1.Incoming<T>) = it.toTransport()
      override suspend fun decode(it: Transport.V1.Incoming<T>) = it.toMessage()
    }

/**
 * Returns a [Coder] for [Message.V1.Outgoing] and [Transport.V1.Outgoing].
 *
 * @param T the type of the event body.
 */
fun <T> Message.V1.Outgoing.Companion.coder() =
    object : Coder<Message.V1.Outgoing<T>, Transport.V1.Outgoing<T>> {
      override suspend fun encode(it: Message.V1.Outgoing<T>) = it.toTransport()
      override suspend fun decode(it: Transport.V1.Outgoing<T>) = it.toMessage()
    }

/**
 * Encodes the [Exchange] to use the [Transport] messages.
 *
 * @param T the type of the event body.
 */
fun <T> Exchange<Message.V1.Incoming<T>, Message.V1.Outgoing<T>>.encode() =
    coding(
        incoming = Message.V1.Incoming.coder(),
        outgoing = Message.V1.Outgoing.coder(),
    )

/**
 * Decodes the [Exchange] to use the [Message] messages.
 *
 * @param T the type of the event body.
 */
fun <T> Exchange<Transport.V1.Incoming<T>, Transport.V1.Outgoing<T>>.decode() =
    coding(
        incoming = Message.V1.Incoming.coder<T>().reversed(),
        outgoing = Message.V1.Outgoing.coder<T>().reversed(),
    )
