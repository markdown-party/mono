package io.github.alexandrepiveteau.echo.ktor

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.ReceiveExchange
import io.github.alexandrepiveteau.echo.SendExchange
import io.github.alexandrepiveteau.echo.channelLink
import io.github.alexandrepiveteau.echo.protocol.Transport.V1.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Transport.V1.Outgoing as Out
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.*
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

@EchoKtorPreview
fun HttpClient.sender(
    sender: HttpRequestBuilder.() -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) = SendExchange {
  channelLink<Inc, Out> { inc ->
    wss(sender) {
      val rcv =
          this.incoming
              .consumeAsFlow()
              .filterIsInstance<Frame.Text>()
              .map { it.readText() }
              .map { Json.decodeFromString(Out.serializer(), it) }
              .flowOn(dispatcher)
              .produceIn(this)

      val snd =
          inc.consumeAsFlow()
              .map { Json.encodeToString(Inc.serializer(), it) }
              .map { Frame.Text(it) }
              .flowOn(dispatcher)
              .produceIn(this)

      // Create the required pipes.
      joinAll(
          pipe(rcv, this@channelLink),
          pipe(snd, this.outgoing),
      )
    }
  }
}

@EchoKtorPreview
fun HttpClient.receiver(
    receiver: HttpRequestBuilder.() -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) = ReceiveExchange {
  channelLink<Out, Inc> { inc ->
    wss(receiver) {
      val rcv =
          this.incoming
              .consumeAsFlow()
              .filterIsInstance<Frame.Text>()
              .map { it.readText() }
              .map { Json.decodeFromString(Inc.serializer(), it) }
              .flowOn(dispatcher)
              .produceIn(this)

      val snd =
          inc.consumeAsFlow()
              .map { Json.encodeToString(Out.serializer(), it) }
              .map { Frame.Text(it) }
              .flowOn(dispatcher)
              .produceIn(this)

      // Create the required pipes.
      joinAll(
          pipe(rcv, this@channelLink),
          pipe(snd, this.outgoing),
      )
    }
  }
}

@EchoKtorPreview
fun HttpClient.exchange(
    receiver: HttpRequestBuilder.() -> Unit,
    sender: HttpRequestBuilder.() -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): Exchange<Inc, Out> =
    object :
        Exchange<Inc, Out>,
        ReceiveExchange<Inc, Out> by receiver(receiver, dispatcher),
        SendExchange<Inc, Out> by sender(sender, dispatcher) {}
