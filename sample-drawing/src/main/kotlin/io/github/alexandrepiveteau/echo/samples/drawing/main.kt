package io.github.alexandrepiveteau.echo.samples.drawing

import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.samples.drawing.data.config.Config
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.DrawingBoardProjection
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.DrawingEvent
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.persistentDrawingBoardOf
import io.github.alexandrepiveteau.echo.samples.drawing.data.runServer
import io.github.alexandrepiveteau.echo.samples.drawing.ui.features.board.Board
import io.github.alexandrepiveteau.echo.samples.drawing.ui.features.board.dashed
import io.github.alexandrepiveteau.echo.samples.drawing.ui.stateful.StatefulParticipants
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** The main entry point of the demo program. */
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
  Window(title = "Echo - Drawing (${config.me.name})", size = IntSize(400, 400)) {
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

        // Let the user add new figures.
        FloatingActionButton(
            onClick = { scope.launch { site.event { yield(DrawingEvent.AddFigure) } } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Outlined.AddBox, null) }

        // Display the figure board, and handle figure modifications.
        Board(
            figures = current,
            onFigureClick = {
              scope.launch {
                val event =
                    DrawingEvent.Move(
                        it.id,
                        toX = Random.nextInt(from = -144, until = 144).dp,
                        toY = Random.nextInt(from = -144, until = 144).dp,
                    )
                site.event { yield(event) }
              }
            },
            onFigureChangeColor = {
              scope.launch {
                val event =
                    DrawingEvent.SetColor(
                        it.id,
                        color = (FiguresColors - it.color).random(),
                    )
                site.event { yield(event) }
              }
            },
            onFigureDelete = { fig ->
              scope.launch { site.event { yield(DrawingEvent.Delete(fig.id)) } }
            },
            modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}

/** The list of [Color] which will be picked for figures. */
private val FiguresColors =
    listOf(
        Color(0xFFFFCDD2),
        Color(0xFFF8BBD0),
        Color(0xFFB39DDB),
        Color(0xFFC5CAE9),
        Color(0xFFB3E5FC),
        Color(0xFFC8E6C9),
        Color(0xFFE6EE9C),
        Color(0xFFFFE082),
    )
