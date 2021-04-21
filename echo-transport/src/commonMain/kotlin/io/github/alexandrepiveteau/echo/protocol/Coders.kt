package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.Exchange

/**
 * Returns a [Coder] for [Message.V1.Incoming] and [Transport.V1.Incoming].
 *
 * @param T the type of the event body.
 */
fun <T> Message.V1.Incoming.Companion.coder(
    coder: Coder<T, String>,
) =
    object : Coder<Message.V1.Incoming<T>, Transport.V1.Incoming> {
      override fun encode(it: Message.V1.Incoming<T>) = it.toTransport(coder::encode)
      override fun decode(it: Transport.V1.Incoming) = it.toMessage(coder::decode)
    }

/**
 * Returns a [Coder] for [Message.V1.Outgoing] and [Transport.V1.Outgoing].
 *
 * @param T the type of the event body.
 */
fun <T> Message.V1.Outgoing.Companion.coder() =
    object : Coder<Message.V1.Outgoing<T>, Transport.V1.Outgoing> {
      override fun encode(it: Message.V1.Outgoing<T>) = it.toTransport()
      override fun decode(it: Transport.V1.Outgoing) = it.toMessage<T>()
    }

/**
 * Encodes the [Exchange] to use the [Transport] messages.
 *
 * @param T the type of the event body.
 */
fun <T> Exchange<Message.V1.Incoming<T>, Message.V1.Outgoing<T>>.encode(
    coder: Coder<T, String>,
) =
    coding(
        incoming = Message.V1.Incoming.coder(coder),
        outgoing = Message.V1.Outgoing.coder(),
    )

/**
 * Decodes the [Exchange] to use the [Message] messages.
 *
 * @param T the type of the event body.
 */
fun <T> Exchange<Transport.V1.Incoming, Transport.V1.Outgoing>.decode(
    coder: Coder<T, String>,
) =
    coding(
        incoming = Message.V1.Incoming.coder(coder).reversed(),
        outgoing = Message.V1.Outgoing.coder<T>().reversed(),
    )
