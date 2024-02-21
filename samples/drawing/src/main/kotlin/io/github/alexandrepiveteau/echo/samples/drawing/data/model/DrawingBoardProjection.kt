package io.github.alexandrepiveteau.echo.samples.drawing.data.model

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.projections.ChangeScope
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection

/**
 * An implementation of a [TwoWayProjection] that generates a [PersistentDrawingBoard] out of a
 * sequence of [DrawingEvent]s.
 */
object DrawingBoardProjection :
    TwoWayProjection<PersistentDrawingBoard, DrawingEvent, DrawingChange> {

  override fun ChangeScope<DrawingChange>.forward(
      model: PersistentDrawingBoard,
      id: EventIdentifier,
      event: DrawingEvent,
  ): PersistentDrawingBoard {
    val (board, change) =
        when (event) {
          is DrawingEvent.AddFigure ->
              model.add(
                  figureId = id,
              )
          is DrawingEvent.Delete ->
              model.delete(
                  figure = event.figure,
              )
          is DrawingEvent.Move ->
              model.move(
                  figure = event.figure,
                  toX = event.toX,
                  toY = event.toY,
              )
          is DrawingEvent.SetColor ->
              model.color(
                  figure = event.figure,
                  color = event.color,
              )
        }
    push(change)
    return board
  }

  override fun backward(
      model: PersistentDrawingBoard,
      id: EventIdentifier,
      event: DrawingEvent,
      change: DrawingChange,
  ): PersistentDrawingBoard = model.reverse(change)
}
