@file:JvmName("Exchanges")
@file:JvmMultifileClass

package io.github.alexandrepiveteau.echo

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Syncs the provided bidirectional flows until they are done communicating. The [sync] operator
 * creates bidirectional communication between the two [Flow] generator functions.
 *
 * The communication stops when both generated [Flow] are completed.
 *
 * @param I the type of the incoming data.
 * @param O the type of the outgoing data.
 */
public suspend fun <I, O> sync(
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
  second(firstToSecond.consumeAsFlow())
      .onEach { secondToFirst.send(it) }
      .onCompletion { secondToFirst.close() }
      .collect()
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
public suspend fun <I, O> sync(vararg exchanges: Exchange<I, O>) {
  return sync(exchanges.asList(), Topology.linear())
}

/**
 * Syncs the provided [Exchange] until they are all done communicating. The [syncAll] operator
 * creates some pairs of [Exchange], forming a fully connected graph.
 *
 * Because a fully connected graph is created, some [Exchange] may transitively sync messages, event
 * if their direct connections are stopped. The degenerate case of this topology is a single
 * exchange, which will not sync at all.
 *
 * @param I the type of the incoming messages.
 * @param O the type of the outgoing messages.
 */
public suspend fun <I, O> syncAll(vararg exchanges: Exchange<I, O>) {
  return sync(exchanges.asList(), Topology.complete())
}

/**
 * A [Topology] defines a mapping between a [List] of sites and a [Sequence] of links between the
 * sites. A [Topology] may introduce some redundancy in the connections between pairs of sites.
 *
 * @param T the type of the elements in the [Topology].
 */
private fun interface Topology<T> {

  /** Maps the [List] of sites to a lazily computed sequence of [Pair]s of sites. */
  fun transform(sites: List<T>): Sequence<Pair<T, T>>

  companion object {

    /** A [Topology] where sites are connected in a line. */
    fun <T> linear() = Topology<T> { it.asSequence().zipWithNext() }

    /** A [Topology] which connects each pair of sites once. */
    fun <T> complete() =
        Topology<T> {
          it.asSequence().flatMapIndexed { i, from ->
            it.asSequence().filterIndexed { j, _ -> i < j }.map { to -> from to to }
          }
        }
  }
}

private suspend fun <I, O> sync(
    exchanges: List<Exchange<I, O>>,
    topology: Topology<Exchange<I, O>>,
): Unit = coroutineScope {
  topology.transform(exchanges).forEach { (left, right) ->
    launch { sync(left::send, right::receive) }
    launch { sync(left::receive, right::send) }
  }
}
