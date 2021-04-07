package io.github.alexandrepiveteau.echo.samples.drawing

import androidx.compose.desktop.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.samples.drawing.data.config.Config
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.DrawingBoardProjection
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.DrawingEvent
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.persistentDrawingBoardOf
import io.github.alexandrepiveteau.echo.samples.drawing.data.runServer
import io.github.alexandrepiveteau.echo.samples.drawing.ui.Board
import io.github.alexandrepiveteau.echo.samples.drawing.ui.features.board.dashed
import io.github.alexandrepiveteau.echo.samples.drawing.ui.stateful.StatefulDashboard
import io.github.alexandrepiveteau.echo.samples.drawing.ui.stateful.StatefulParticipants
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {

  // The single source of truth for the local site.
  val site =
      mutableSite(
          identifier = SiteIdentifier.random(),
          initial = persistentDrawingBoardOf(),
          projection = DrawingBoardProjection,
      )

  // Parse the launch configuration.
  val config = Config.parse(args) ?: exitProcess(1)

  // Start a server.
  runServer(site, config)

  // Run the GUI.
  val figures = site.value.map { it.figures }
  Window(title = "Echo - Drawing (${config.me.name})") {
    MaterialTheme {
      Box(Modifier.fillMaxSize().dashed()) {
        val current by figures.collectAsState(persistentSetOf())
        val scope = rememberCoroutineScope()

        // Display the participants board.
        StatefulParticipants(
            site = site,
            participants = config.participant,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        )

        // Display the figure board.
        Board(
            figures = current,
            onFigureClick = {
              scope.launch {
                val event =
                    DrawingEvent.Move(
                        it.id,
                        toX = Random.nextInt(from = -300, until = 300).dp,
                        toY = Random.nextInt(from = -300, until = 300).dp,
                    )
                site.event { yield(event) }
              }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Display the debug inputs.
        Row {
          Button(
              onClick = { scope.launch { site.event { yield(DrawingEvent.AddFigure) } } },
          ) { Text("${current.size} figures") }
          StatefulDashboard(
              site = site,
              modifier = Modifier.width(300.dp).background(Color.LightGray),
          )
        }
      }
    }
  }
}
