package io.github.alexandrepiveteau.echo

import kotlinx.coroutines.flow.Flow

/**
 * An interface defining an asymmetrical replication site, biased towards sending data.
 *
 * @param I the type of the domain-specific incoming events for this [SendExchange].
 * @param O the type of the domain-specific outgoing events for this [SendExchange].
 */
public fun interface SendExchange<in I, out O> {

  /** Sends some [O] as a response to an [incoming] [Flow] of [I]. */
  public fun send(incoming: Flow<I>): Flow<O>
}

/** Returns the [SendExchange]. */
public fun <I, O> SendExchange<I, O>.asSendExchange(): SendExchange<I, O> = SendExchange(this::send)

/**
 * An interface defining an asymmetrical replication site, biased towards receiving data.
 *
 * @param I the type of the domain-specific incoming events for this [ReceiveExchange].
 * @param O the type of the domain-specific outgoing events for this [ReceiveExchange].
 */
public fun interface ReceiveExchange<out I, in O> {

  /** Sends some [I] as a response to an [incoming] [Flow] of [O]. */
  public fun receive(incoming: Flow<O>): Flow<I>
}

/** Returns the [ReceiveExchange]. */
public fun <I, O> ReceiveExchange<I, O>.asReceiveExchange(): ReceiveExchange<I, O> =
    ReceiveExchange(this::receive)

/**
 * An interface defining an [Exchange], which is able to generate some flows that are used for
 * bidirectional communication and transmission of data.
 *
 * @param I the type of the domain-specific incoming events for this [Exchange].
 * @param O the type of the domain-specific outgoing events for this [Exchange].
 */
public interface Exchange<I, O> : SendExchange<I, O>, ReceiveExchange<I, O>
