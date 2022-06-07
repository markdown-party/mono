package io.github.alexandrepiveteau.echo.ktor

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.ReceiveExchange
import io.github.alexandrepiveteau.echo.SendExchange
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
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

fun HttpClient.wssSendExchange(
    sender: HttpRequestBuilder.() -> Unit,
): SendExchange<Frame, Frame> = SendExchange { inc ->
  channelFlow {
    wss(sender) {
      sender(
              inc = inc.produceIn(this),
              out = this@channelFlow,
              socketInc = incoming,
              socketOut = outgoing,
          )
          .join()
    }
  }
}

fun HttpClient.wsSendExchange(
    sender: HttpRequestBuilder.() -> Unit,
): SendExchange<Frame, Frame> = SendExchange { inc ->
  channelFlow {
    ws(sender) {
      sender(
              inc = inc.produceIn(this),
              out = this@channelFlow,
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

fun HttpClient.wssReceiveExchange(
    receiver: HttpRequestBuilder.() -> Unit,
): ReceiveExchange<Frame, Frame> = ReceiveExchange { inc ->
  channelFlow {
    wss(receiver) {
      receiver(
              inc = inc.produceIn(this),
              out = this@channelFlow,
              socketInc = incoming,
              socketOut = outgoing,
          )
          .join()
    }
  }
}

fun HttpClient.wsReceiveExchange(
    receiver: HttpRequestBuilder.() -> Unit,
): ReceiveExchange<Frame, Frame> = ReceiveExchange { inc ->
  channelFlow {
    ws(receiver) {
      receiver(
              inc = inc.produceIn(this),
              out = this@channelFlow,
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

fun HttpClient.wssExchange(
    receiver: HttpRequestBuilder.() -> Unit,
    sender: HttpRequestBuilder.() -> Unit,
): Exchange<Frame, Frame> =
    DelegatingExchange(
        s = wssSendExchange(sender),
        r = wssReceiveExchange(receiver),
    )

fun HttpClient.wsExchange(
    receiver: HttpRequestBuilder.() -> Unit,
    sender: HttpRequestBuilder.() -> Unit,
): Exchange<Frame, Frame> =
    DelegatingExchange(
        s = wsSendExchange(sender),
        r = wsReceiveExchange(receiver),
    )
