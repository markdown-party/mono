package io.github.alexandrepiveteau.echo

import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow

/**
 * Tests the given [Flow] to [Flow] function. More specifically, a [FlowTurbineSendChannel] will be
 * started on the flows.
 *
 * @param timeout the [FlowTurbine] timeout.
 * @param validate the validation block.
 */
suspend fun <A, B> ((Flow<A>) -> Flow<B>).test(
    timeoutMillis: Long = 1000,
    validate: suspend FlowTurbineSendChannel<B, A>.() -> Unit,
) {
  val buf = Channel<A>()
  this(buf.consumeAsFlow()).test(timeoutMillis) { validate(FlowTurbineCollectorImpl(this, buf)) }
}

/**
 * An interface which combines [FlowTurbine] with a [SendChannel] to test bi-directional sync. When
 * a function takes an input [Flow] and transforms it into an output [Flow], the
 * [FlowTurbineSendChannel] gives you the ability to make assertions on the output while sending
 * input.
 */
interface FlowTurbineSendChannel<A, B> : FlowTurbine<A>, SendChannel<B>

/**
 * An implementation of [FlowTurbineSendChannel] which delegates to some distinct [FlowTurbine] and
 * [SendChannel] instances.
 */
private class FlowTurbineCollectorImpl<A, B>(
    turbine: FlowTurbine<A>,
    producer: SendChannel<B>,
) : FlowTurbineSendChannel<A, B>, FlowTurbine<A> by turbine, SendChannel<B> by producer
