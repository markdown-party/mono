package party.markdown

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.projections.ChangeScope
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection
import party.markdown.MarkdownEvent.Decrement
import party.markdown.MarkdownEvent.Increment

object MarkdownProjection : TwoWayProjection<Int, MarkdownEvent, MarkdownEvent> {

  private fun update(current: Int, event: MarkdownEvent, factor: Int = 1): Int =
      when (event) {
        Increment -> current + (1 * factor)
        Decrement -> current - (1 * factor)
      }

  override fun ChangeScope<MarkdownEvent>.forward(
      model: Int,
      id: EventIdentifier,
      event: MarkdownEvent
  ): Int {
    push(event)
    return update(model, event)
  }

  override fun backward(
      model: Int,
      id: EventIdentifier,
      event: MarkdownEvent,
      change: MarkdownEvent
  ): Int {
    return update(model, change, -1)
  }
}
