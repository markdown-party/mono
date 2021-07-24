package io.github.alexandrepiveteau.echo.samples.drawing.data

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.ktor.server.receiver
import io.github.alexandrepiveteau.echo.samples.drawing.data.config.Config
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.DrawingEvent
import io.github.alexandrepiveteau.echo.serialization.encodeToFrame
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
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
) {
  launch(Dispatchers.IO) {
    embeddedServer(CIO, port = config.me.port) {
          install(WebSockets)
          routing { route("/sync") { receiver { site.encodeToFrame() } } }
        }
        .start()
  }
}
