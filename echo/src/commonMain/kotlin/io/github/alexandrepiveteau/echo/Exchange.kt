package io.github.alexandrepiveteau.echo

import kotlinx.coroutines.flow.Flow

/**
 * An interface defining an asymmetrical replication site, biased towards sending data.
 *
 * @param I the type of the domain-specific incoming events for this [SendExchange].
 * @param O the type of the domain-specific outgoing events for this [SendExchange].
 */
fun interface SendExchange<in I, out O> {

  /** Sends some [O] as a response to an [incoming] [Flow] of [I]. */
  fun send(incoming: Flow<I>): Flow<O>
}

/**
 * An interface defining an asymmetrical replication site, biased towards receiving data.
 *
 * @param I the type of the domain-specific incoming events for this [ReceiveExchange].
 * @param O the type of the domain-specific outgoing events for this [ReceiveExchange].
 */
fun interface ReceiveExchange<out I, in O> {

  /** Sends some [I] as a response to an [incoming] [Flow] of [O]. */
  fun receive(incoming: Flow<O>): Flow<I>
}

/**
 * An interface defining an [Exchange], which is able to generate some flows that are used for
 * bidirectional communication and transmission of data.
 *
 * @param I the type of the domain-specific incoming events for this [Exchange].
 * @param O the type of the domain-specific outgoing events for this [Exchange].
 */
interface Exchange<I, O> : SendExchange<I, O>, ReceiveExchange<I, O>
