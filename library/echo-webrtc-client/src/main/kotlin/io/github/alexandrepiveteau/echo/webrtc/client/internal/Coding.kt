package io.github.alexandrepiveteau.echo.webrtc.client.internal

import io.github.alexandrepiveteau.echo.*
import kotlinx.coroutines.flow.map
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.decodeFromHexString as decodeHex
import kotlinx.serialization.encodeToHexString as encodeHex

internal inline fun <reified A, reified B> SendExchange<A, B>.encode(
    format: BinaryFormat = DefaultBinaryFormat,
): SendExchange<String, String> = SendExchange { incoming ->
  this@encode.send(incoming.map(format::decodeHex)).map(format::encodeHex)
}

internal inline fun <reified A, reified B> ReceiveExchange<A, B>.encode(
    format: BinaryFormat = DefaultBinaryFormat,
): ReceiveExchange<String, String> = ReceiveExchange { incoming ->
  this@encode.receive(incoming.map(format::decodeHex)).map(format::encodeHex)
}

/**
 * Encodes the given [Exchange] to an [Exchange] of [String].
 *
 * @param A the type of the incoming messages.
 * @param B the type of the outgoing messages.
 * @param format the [BinaryFormat] to use when encoding the exchange.
 * @return the encoded [Exchange].
 */
internal inline fun <reified A, reified B> Exchange<A, B>.encode(
    format: BinaryFormat = DefaultBinaryFormat,
): Exchange<String, String> =
    object :
        Exchange<String, String>,
        ReceiveExchange<String, String> by asReceiveExchange().encode(format),
        SendExchange<String, String> by asSendExchange().encode(format) {}
