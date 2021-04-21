package io.github.alexandrepiveteau.echo.ktor

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.ReceiveExchange
import io.github.alexandrepiveteau.echo.SendExchange
import io.github.alexandrepiveteau.echo.channelLink
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

// PIPING JOBS

/** Pipes an [ReceiveChannel] into a [SendChannel]. */
private fun <T> CoroutineScope.pipe(
    incoming: ReceiveChannel<T>,
    outgoing: SendChannel<T>,
): Job = launch {
  incoming.consumeAsFlow().onEach { outgoing.send(it) }.onCompletion { outgoing.close() }.collect()
}

// SENDERS

/**
 * A block that abstracts the sender part of the websocket. This will be changed to a multi-receiver
 * function when they are supported in Kotlin.
 */
private fun CoroutineScope.sender(
    inc: ReceiveChannel<Frame>,
    out: SendChannel<Frame>,
    socketInc: ReceiveChannel<Frame>,
    socketOut: SendChannel<Frame>,
): Job {
  val rcv = socketInc.consumeAsFlow().produceIn(this)
  val snd = inc.consumeAsFlow().produceIn(this)

  // Create the required pipes.
  return launch {
    joinAll(
        pipe(rcv, out),
        pipe(snd, socketOut),
    )
  }
}

@EchoKtorPreview
fun HttpClient.wssSendExchange(
    sender: HttpRequestBuilder.() -> Unit,
) = SendExchange {
  channelLink<Frame, Frame> { inc ->
    wss(sender) {
      sender(
              inc = inc,
              out = this@channelLink,
              socketInc = incoming,
              socketOut = outgoing,
          )
          .join()
    }
  }
}

@EchoKtorPreview
fun HttpClient.wsSendExchange(
    sender: HttpRequestBuilder.() -> Unit,
) = SendExchange {
  channelLink<Frame, Frame> { inc ->
    ws(sender) {
      sender(
              inc = inc,
              out = this@channelLink,
              socketInc = incoming,
              socketOut = outgoing,
          )
          .join()
    }
  }
}

// RECEIVERS

/**
 * A block that abstracts the receiver part of the websocket. This will be changed to a
 * multi-receiver function when they are supported in Kotlin.
 */
private fun CoroutineScope.receiver(
    inc: ReceiveChannel<Frame>,
    out: SendChannel<Frame>,
    socketInc: ReceiveChannel<Frame>,
    socketOut: SendChannel<Frame>,
): Job {
  val rcv = socketInc.consumeAsFlow().produceIn(this)
  val snd = inc.consumeAsFlow().produceIn(this)

  // Create the required pipes.
  return launch {
    joinAll(
        pipe(rcv, out),
        pipe(snd, socketOut),
    )
  }
}

@EchoKtorPreview
fun HttpClient.wssReceiveExchange(
    receiver: HttpRequestBuilder.() -> Unit,
) = ReceiveExchange {
  channelLink<Frame, Frame> { inc ->
    wss(receiver) {
      receiver(
              inc = inc,
              out = this@channelLink,
              socketInc = incoming,
              socketOut = outgoing,
          )
          .join()
    }
  }
}

@EchoKtorPreview
fun HttpClient.wsReceiveExchange(
    receiver: HttpRequestBuilder.() -> Unit,
) = ReceiveExchange {
  channelLink<Frame, Frame> { inc ->
    ws(receiver) {
      receiver(
              inc = inc,
              out = this@channelLink,
              socketInc = incoming,
              socketOut = outgoing,
          )
          .join()
    }
  }
}

// COMBINED

private class DelegatingExchange<I, O>(
    private val s: SendExchange<I, O>,
    private val r: ReceiveExchange<I, O>,
) : Exchange<I, O>, SendExchange<I, O> by s, ReceiveExchange<I, O> by r

@EchoKtorPreview
fun HttpClient.wssExchange(
    receiver: HttpRequestBuilder.() -> Unit,
    sender: HttpRequestBuilder.() -> Unit,
): Exchange<Frame, Frame> =
    DelegatingExchange(
        s = wssSendExchange(sender),
        r = wssReceiveExchange(receiver),
    )

@EchoKtorPreview
fun HttpClient.wsExchange(
    receiver: HttpRequestBuilder.() -> Unit,
    sender: HttpRequestBuilder.() -> Unit,
): Exchange<Frame, Frame> =
    DelegatingExchange(
        s = wsSendExchange(sender),
        r = wsReceiveExchange(receiver),
    )
