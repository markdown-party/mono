package io.github.alexandrepiveteau.echo.samples.drawing.ui.features.board

import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Draws a series of dashed dots, centered on the current board, which span across the whole drawing
 * surface. The dots form a grid-like pattern.
 *
 * @param radius the radius of each dot.
 * @param color the color of the dots.
 * @param grid the size of each grid cell.
 */
fun Modifier.dashed(
    radius: Dp = 2.dp,
    color: Color? = null,
    grid: Dp = 48.dp,
) = composed {
  val requiredColor = color ?: LocalContentColor.current
  val alpha = ContentAlpha.disabled
  drawBehind {
    val origin = Offset(center.x.rem(grid.toPx()), center.y.rem(grid.toPx()))
    val x = (size.width / grid.toPx()).roundToInt()
    val y = (size.height / grid.toPx()).roundToInt()
    for (i in 0..x) {
      for (j in 0..y) {
        drawCircle(
            color = requiredColor,
            radius = radius.toPx(),
            center = origin + Offset(i * grid.toPx(), j * grid.toPx()),
            alpha = alpha,
        )
      }
    }
  }
}
