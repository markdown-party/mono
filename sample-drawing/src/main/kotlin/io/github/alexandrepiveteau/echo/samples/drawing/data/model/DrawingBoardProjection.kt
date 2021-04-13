package io.github.alexandrepiveteau.echo.samples.drawing.data.model

import io.github.alexandrepiveteau.echo.logs.EventLog
import io.github.alexandrepiveteau.echo.projections.OneWayProjection

/**
 * An implementation of a [OneWayProjection] that generates a [PersistentDrawingBoard] out of a
 * sequence of [EventLog.Entry].
 */
object DrawingBoardProjection :
    OneWayProjection<PersistentDrawingBoard, EventLog.Entry<DrawingEvent>> {

  override fun forward(
      body: EventLog.Entry<DrawingEvent>,
      model: PersistentDrawingBoard,
  ): PersistentDrawingBoard {

    // Return an updated PersistentDrawingBoard.
    return when (val event = body.body) {
      is DrawingEvent.AddFigure -> model.add(figureId = body.identifier)
      is DrawingEvent.Delete -> model.delete(figure = event.figure)
      is DrawingEvent.Move -> model.move(figure = event.figure, toX = event.toX, toY = event.toY)
      is DrawingEvent.SetColor -> model.color(figure = event.figure, color = event.color)
    }
  }
}
