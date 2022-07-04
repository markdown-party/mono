package io.github.alexandrepiveteau.echo.ktor.server

import io.github.alexandrepiveteau.echo.ReceiveExchange
import io.github.alexandrepiveteau.echo.SendExchange
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

fun Route.sender(
    block: suspend DefaultWebSocketServerSession.() -> SendExchange<Frame, Frame>,
) {
  webSocket {
    block()
        .send(this.incoming.consumeAsFlow())
        .onEach(outgoing::send)
        .onCompletion { outgoing.close() }
        .collect()
  }
}

fun Route.receiver(
    block: suspend DefaultWebSocketServerSession.() -> ReceiveExchange<Frame, Frame>,
) {
  webSocket {
    block()
        .receive(this.incoming.consumeAsFlow())
        .onEach(outgoing::send)
        .onCompletion { outgoing.close() }
        .collect()
  }
}
