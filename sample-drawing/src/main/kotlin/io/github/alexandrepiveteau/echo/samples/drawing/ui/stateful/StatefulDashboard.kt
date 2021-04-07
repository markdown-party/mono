package io.github.alexandrepiveteau.echo.samples.drawing.ui.stateful

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.ktor.exchange
import io.github.alexandrepiveteau.echo.protocol.decode
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.DrawingEvent
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.PersistentDrawingBoard
import io.github.alexandrepiveteau.echo.samples.drawing.ui.Dashboard
import io.github.alexandrepiveteau.echo.sync
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay

private val HttpClient = HttpClient(CIO) { install(WebSockets) }

private val Remote =
    HttpClient.exchange(
            receiver = {
              host = "localhost"
              port = 8080
              url { path("receiver") }
            },
            sender = {
              host = "localhost"
              port = 8080
              url { path("sender") }
            },
        )
        .decode(DrawingEvent)

/**
 * A composable that implements state over [Dashboard].
 *
 * @param site the [MutableSite] that acts as the local source of truth for data.
 * @param modifier the [Modifier] for this composable.
 */
@Composable
fun StatefulDashboard(
    site: MutableSite<DrawingEvent, PersistentDrawingBoard>,
    modifier: Modifier = Modifier,
) {
  var syncing by remember { mutableStateOf(false) }

  LaunchedEffect(syncing) {
    if (syncing) {
      try {
        sync(site, Remote)
      } catch (error: Throwable) {
        error.printStackTrace()
      } finally {
        delay(1000) // Make sync end less abrupt.
        syncing = false
      }
    }
  }

  Dashboard(
      syncing = syncing,
      onSyncingToggled = { syncing = it },
      modifier = modifier,
  )
}
