package io.github.alexandrepiveteau.echo.samples.drawing.ui.stateful

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.ktor.receiver
import io.github.alexandrepiveteau.echo.protocol.encode
import io.github.alexandrepiveteau.echo.samples.drawing.data.config.Participant
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.DrawingEvent
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.PersistentDrawingBoard
import io.github.alexandrepiveteau.echo.samples.drawing.ui.features.network.Participant as Cell
import io.github.alexandrepiveteau.echo.sync
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay

private val HttpClient = HttpClient(CIO) { install(WebSockets) }

@Composable
fun StatefulParticipants(
    site: MutableSite<DrawingEvent, PersistentDrawingBoard>,
    participants: List<Participant>,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier,
      Arrangement.spacedBy(16.dp),
      Alignment.End,
  ) {
    for (participant in participants) {
      var syncing by remember { mutableStateOf(false) }

      // Sync with the provided site.
      LaunchedEffect(syncing) {
        if (syncing) {
          try {
            // TODO : Eventually extract this logic somewhere.
            val receiver =
                HttpClient.receiver({
                  host = participant.host
                  port = participant.port
                  url { path(participant.path) }
                })
            sync(site.encode(DrawingEvent).outgoing(), receiver.incoming())
          } catch (problem: Throwable) {
            problem.printStackTrace()
          } finally {
            delay(1000)
            syncing = false
          }
        }
      }

      // Display the participant info.
      Cell(
          name = participant.name,
          connected = syncing,
          onClick = { syncing = !syncing },
      )
    }
  }
}
