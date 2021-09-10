@file:Suppress("DuplicatedCode")

package io.github.alexandrepiveteau.echo

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Syncs the provided bi-directional flows until they are done communicating. The [sync] operator
 * creates bidirectional communication between the two [Flow] generator functions, which communicate
 * with some [Channel] .
 *
 * The communication stops when both generated [Flow] are completed.
 *
 * @param I the type of the incoming data.
 * @param O the type of the outgoing data.
 */
suspend fun <I, O> sync(
    first: (Flow<I>) -> Flow<O>,
    second: (Flow<O>) -> Flow<I>,
): Unit = coroutineScope {
  val firstToSecond = Channel<O>()
  val secondToFirst = Channel<I>()
  launch {
    first(secondToFirst.consumeAsFlow())
        .onEach { firstToSecond.send(it) }
        .onCompletion { firstToSecond.close() }
        .collect()
  }
  launch {
    second(firstToSecond.consumeAsFlow())
        .onEach { secondToFirst.send(it) }
        .onCompletion { secondToFirst.close() }
        .collect()
  }
}

/**
 * Syncs the provided [Exchange] until they are all done communicating. The [sync] operator creates
 * a chain of [Exchange], and for each pair of the chain, some flows that are then used for
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
  exchanges.asSequence().zipWithNext().forEach { (left, right) ->
    launch { sync(left::send, right::receive) }
    launch { sync(left::receive, right::send) }
  }
}
