@file:Suppress("DuplicatedCode")

package io.github.alexandrepiveteau.echo

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Syncs the provided [Link] instances until they are done communicating. The [sync] operator
 * creates bidirectional communication between the two [Link], which communicate with some [Channel]
 * .
 *
 * The communication stops when both [Link] are completed.
 *
 * @param I the type of the incoming data.
 * @param O the type of the outgoing data.
 */
suspend fun <I, O> sync(
    first: Link<I, O>,
    second: Link<O, I>,
): Unit = coroutineScope {
  val fToS = Channel<O>()
  val sToF = Channel<I>()
  launch {
    first
        .talk(sToF.consumeAsFlow())
        .onEach { fToS.send(it) }
        .onCompletion { fToS.close() }
        .collect()
  }
  launch {
    second
        .talk(fToS.consumeAsFlow())
        .onEach { sToF.send(it) }
        .onCompletion { sToF.close() }
        .collect()
  }
}

/**
 * Syncs the provided [Exchange] until they are all done communicating. The [sync] operator creates
 * a chain of [Exchange], and for each pair of the chain, some [Link] that are then used for
 * communication until all the data is eventually synced.
 *
 * Because a chain is created, if an [Exchange] stops exchanging messages in the middle, the
 * extremities of the chain will not be able to communicate anymore. The degenerate case of the
 * chain is a pair of [Exchange], which will simply exchange until they're done syncing.
 *
 * @param I the type of the incoming messages.
 * @param O the type of the outgoing messages.
 */
suspend fun <I, O> sync(
    vararg exchanges: Exchange<I, O>,
): Unit = coroutineScope {
  exchanges.asSequence().windowed(2).forEach { (left, right) ->
    launch { sync(left.incoming(), right.outgoing()) }
    launch { sync(left.outgoing(), right.incoming()) }
  }
}
