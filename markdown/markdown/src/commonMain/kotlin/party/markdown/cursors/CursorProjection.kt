package party.markdown.cursors

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import party.markdown.MarkdownPartyEvent
import party.markdown.MarkdownPartyEvent.*
import party.markdown.cursors.CursorEvent.MoveAfter
import party.markdown.rga.RGAEvent.Insert

object CursorProjection : OneWayProjection<MutableCursors, MarkdownPartyEvent> {

  override fun forward(
      model: MutableCursors,
      identifier: EventIdentifier,
      event: MarkdownPartyEvent,
  ): MutableCursors {
    when (event) {
      is Cursor ->
          when (event.event) {
            is MoveAfter ->
                model.move(
                    id = identifier,
                    node = event.event.node,
                    anchor = event.event.anchor,
                )
          }
      is Tree -> Unit // Ignored.
      is RGA ->
          when (event.event) {
            is Insert ->
                model.insert(
                    id = identifier,
                    node = event.document,
                    anchor = event.event.offset,
                )
            else -> Unit
          }
    }
    return model
  }
}
