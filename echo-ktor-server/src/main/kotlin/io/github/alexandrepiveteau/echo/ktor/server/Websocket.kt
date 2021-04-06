package io.github.alexandrepiveteau.echo.ktor.server

import io.github.alexandrepiveteau.echo.ReceiveExchange
import io.github.alexandrepiveteau.echo.SendExchange
import io.github.alexandrepiveteau.echo.protocol.Transport.V1.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Transport.V1.Outgoing as Out
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

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
    exchange: SendExchange<Inc, Out>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  webSocket {
    val rcv =
        this.incoming
            .consumeAsFlow()
            .filterIsInstance<Frame.Text>()
            .map { it.readText() }
            .map { Json.decodeFromString(Inc.serializer(), it) }
            .flowOn(dispatcher)

    val snd =
        exchange
            .outgoing()
            .talk(rcv)
            .map { Json.encodeToString(Out.serializer(), it) }
            .map { Frame.Text(it) }
            .flowOn(dispatcher)
            .produceIn(this)

    pipe(snd, this.outgoing).join()
  }
}

@EchoKtorServerPreview
@OptIn(FlowPreview::class)
fun Route.receiver(
    exchange: ReceiveExchange<Inc, Out>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  webSocket {
    val rcv =
        this.incoming
            .consumeAsFlow()
            .filterIsInstance<Frame.Text>()
            .map { it.readText() }
            .map { Json.decodeFromString(Out.serializer(), it) }
            .flowOn(dispatcher)

    val snd =
        exchange
            .incoming()
            .talk(rcv)
            .map { Json.encodeToString(Inc.serializer(), it) }
            .map { Frame.Text(it) }
            .flowOn(dispatcher)
            .produceIn(this)

    pipe(snd, this.outgoing).join()
  }
}
