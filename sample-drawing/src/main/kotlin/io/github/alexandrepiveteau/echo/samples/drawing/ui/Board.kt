package io.github.alexandrepiveteau.echo.samples.drawing.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.alexandrepiveteau.echo.samples.drawing.data.model.Figure

@Composable
fun Board(
    figures: Set<Figure>,
    onFigureClick: (Figure) -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(modifier, Alignment.Center) {
    for (figure in figures) {
      key(figure.id) {
        val color by animateColorAsState(figure.color)
        val x by animateDpAsState(figure.offset.x)
        val y by animateDpAsState(figure.offset.y)

        Figure(
            color = color,
            onClick = { onFigureClick(figure) },
            modifier = Modifier.offset { IntOffset(x.roundToPx(), y.roundToPx()) },
        )
      }
    }
  }
}

@Composable
fun Figure(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Surface(
      modifier = modifier.size(96.dp),
      color = color,
      elevation = 2.dp,
  ) { Box(Modifier.fillMaxSize().clickable { onClick() }) }
}
