package party.markdown

import io.github.alexandrepiveteau.echo.logs.EventLog.IndexedEvent
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import party.markdown.MarkdownEvent.Decrement
import party.markdown.MarkdownEvent.Increment

object MarkdownProjection : OneWayProjection<Int, IndexedEvent<MarkdownEvent>> {

  override fun forward(
      body: IndexedEvent<MarkdownEvent>,
      model: Int,
  ) =
      when (body.body) {
        Increment -> model + 1
        Decrement -> model - 1
      }
}
