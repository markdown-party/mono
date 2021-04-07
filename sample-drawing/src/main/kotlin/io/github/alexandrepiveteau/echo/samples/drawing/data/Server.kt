package io.github.alexandrepiveteau.echo.samples.drawing.data

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.ktor.server.receiver
import io.github.alexandrepiveteau.echo.protocol.encode
import io.github.alexandrepiveteau.echo.samples.drawing.data.config.Config
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.DrawingEvent
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Runs a server for the provided [Config], creating a single "sync" sender endpoint.
 *
 * @param site the [MutableSite] to sync with.
 * @param config the [Config] that defines the server properties.
 */
fun CoroutineScope.runServer(
    site: MutableSite<DrawingEvent, *>,
    config: Config,
) =
    launch(Dispatchers.IO) {
      val server =
          embeddedServer(Netty, port = config.me.port) {
            install(WebSockets)
            routing { route("/sync") { receiver(site.encode(DrawingEvent)) } }
          }
      server.start(wait = true)
    }
