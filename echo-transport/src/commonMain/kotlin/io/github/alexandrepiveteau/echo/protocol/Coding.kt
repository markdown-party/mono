package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.Link
import kotlinx.coroutines.flow.map

/**
 * Transforms an [Link] with a bi-directional function, such that the inner messages are all coded
 * differently. The [coding] combinator does not allow encoding and decoding failures; therefore,
 * such failures should be thrown as exceptions and managed by the [Link] implementations or their
 * callers directly.
 *
 * @param incoming transforms the incoming messages to the existing message type.
 * @param outgoing transforms the outgoing messages to the new message type.
 */
fun <I1, O1, I2, O2> Link<I1, O1>.coding(
    incoming: (I2) -> I1,
    outgoing: (O1) -> O2,
): Link<I2, O2> = Link { this.talk(it.map { e -> incoming(e) }).map { e -> outgoing(e) } }

/**
 * A simple bi-directional encoding and decoding mechanism.
 *
 * @param I the type of the unencoded content.
 * @param O the type of the encoded content.
 */
interface Coder<I, O> {
    fun encode(it: I): O
    fun decode(it: O): I
}

/** Reverses a [Coder] implementation. */
fun <I, O> Coder<I, O>.reversed(): Coder<O, I> =
    object : Coder<O, I> {
        override fun encode(it: O) = this@reversed.decode(it)
        override fun decode(it: I) = this@reversed.encode(it)
    }

/**
 * Transforms an [Exchange] with a pair of bi-directional functions, such that the incoming messages
 * are transformed to a certain type, and the outgoing messages are transformed to another type. The
 * [coding] combinator does not allow encoding and decoding failures; therefore, such failures
 * should be thrown as exceptions and managed by the [Exchange] callers directly.
 *
 * @param incoming a [Coder] acting as a bijection between [I1] and [I2].
 * @param outgoing a [Coder] acting as a bijection between [O1] and [O2].
 */
fun <I1, O1, I2, O2> Exchange<I1, O1>.coding(
    incoming: Coder<I1, I2>,
    outgoing: Coder<O1, O2>,
): Exchange<I2, O2> =
    object : Exchange<I2, O2> {
        override fun outgoing(): Link<I2, O2> =
            this@coding.outgoing()
                .coding(
                    incoming = incoming::decode,
                    outgoing = outgoing::encode,
                )
        override fun incoming(): Link<O2, I2> =
            this@coding.incoming()
                .coding(
                    incoming = outgoing::decode,
                    outgoing = incoming::encode,
                )
    }
