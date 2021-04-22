package io.github.alexandrepiveteau.echo.ktor.server

import io.github.alexandrepiveteau.echo.ReceiveExchange
import io.github.alexandrepiveteau.echo.SendExchange
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Pipes an [ReceiveChannel] into a [SendChannel]. */
private fun <T> CoroutineScope.pipe(
    incoming: ReceiveChannel<T>,
    outgoing: SendChannel<T>,
): Job = launch {
  incoming.consumeAsFlow().onEach { outgoing.send(it) }.onCompletion { outgoing.close() }.collect()
}

@EchoKtorServerPreview
@OptIn(FlowPreview::class)
fun Route.sender(
    exchange: SendExchange<Frame, Frame>,
) {
  webSocket {
    val rcv = this.incoming.consumeAsFlow()
    val snd = exchange.outgoing().talk(rcv).produceIn(this)

    pipe(snd, this.outgoing).join()
  }
}

@EchoKtorServerPreview
@OptIn(FlowPreview::class)
fun Route.receiver(
    exchange: ReceiveExchange<Frame, Frame>,
) {
  webSocket {
    val rcv = this.incoming.consumeAsFlow()
    val snd = exchange.incoming().talk(rcv).produceIn(this)

    pipe(snd, this.outgoing).join()
  }
}
