package io.github.alexandrepiveteau.echo.ktor

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.channelLink
import io.github.alexandrepiveteau.echo.protocol.Transport
import io.github.alexandrepiveteau.echo.protocol.Transport.V1.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Transport.V1.Outgoing as Out
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Pipes an [ReceiveChannel] into a [SendChannel]. */
private fun <T> CoroutineScope.pipe(
    incoming: ReceiveChannel<T>,
    outgoing: SendChannel<T>,
): Job = launch {
  incoming.consumeAsFlow().onEach { outgoing.send(it) }.onCompletion { outgoing.close() }
}

@EchoKtorPreview
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
fun <T> HttpClient.exchange(
    incoming: HttpRequestBuilder.() -> Unit,
    outgoing: HttpRequestBuilder.() -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) =
    object : Exchange<Inc<T>, Out<T>> {

      override fun incoming() =
          channelLink<Out<T>, Inc<T>> { inc ->
            this@exchange.wss(incoming) {
              val rcv =
                  this.incoming
                      .consumeAsFlow()
                      .filterIsInstance<Frame.Text>()
                      .map { it.readText() }
                      .map { Json.decodeFromString<Transport.V1.Incoming<T>>(it) }
                      .flowOn(dispatcher)
                      .produceIn(this)

              val snd =
                  inc.consumeAsFlow()
                      .map { Json.encodeToString(it) }
                      .map { Frame.Text(it) }
                      .flowOn(dispatcher)
                      .produceIn(this)

              // Create the required pipes.
              pipe(rcv, this@channelLink)
              pipe(snd, this.outgoing)
            }
          }

      override fun outgoing() =
          channelLink<Inc<T>, Out<T>> { inc ->
            wss(outgoing) {
              val rcv =
                  this.incoming
                      .consumeAsFlow()
                      .filterIsInstance<Frame.Text>()
                      .map { it.readText() }
                      .map { Json.decodeFromString<Transport.V1.Outgoing<T>>(it) }
                      .flowOn(dispatcher)
                      .produceIn(this)

              val snd =
                  inc.consumeAsFlow()
                      .map { Json.encodeToString(it) }
                      .map { Frame.Text(it) }
                      .flowOn(dispatcher)
                      .produceIn(this)

              // Create the required pipes.
              pipe(rcv, this@channelLink)
              pipe(snd, this.outgoing)
            }
          }
    }
