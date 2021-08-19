package io.github.alexandrepiveteau.echo.ktor.server

import io.github.alexandrepiveteau.echo.ReceiveExchange
import io.github.alexandrepiveteau.echo.SendExchange
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

@EchoKtorServerPreview
@OptIn(FlowPreview::class)
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

@EchoKtorServerPreview
@OptIn(FlowPreview::class)
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
