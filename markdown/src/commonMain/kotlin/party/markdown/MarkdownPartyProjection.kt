package party.markdown

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.projections.ChangeScope
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection
import party.markdown.MarkdownPartyChange as Change
import party.markdown.MarkdownPartyEvent as Event
import party.markdown.MutableMarkdownParty as Model
import party.markdown.rga.MutableRGA
import party.markdown.rga.RGAProjection
import party.markdown.tree.TreeProjection

object MarkdownPartyProjection : TwoWayProjection<Model, Event, Change> {

  override fun ChangeScope<Change>.forward(
      model: Model,
      id: EventIdentifier,
      event: Event,
  ): Model {
    when (event) {
      is Event.Tree -> with(TreeProjection) { forward(model.tree, id, event.event) }
      is Event.RGA -> {
        val rga = model.files.getOrPut(event.document) { MutableRGA() }
        with(RGAProjection) { forward(rga, id, event.event) }
      }
    }
    return model
  }

  override fun backward(
      model: Model,
      id: EventIdentifier,
      event: Event,
      change: Change,
  ): Model {
    when (event) {
      is Event.Tree ->
          TreeProjection.backward(
              model = model.tree,
              id = id,
              event = event.event,
              change = change,
          )
      is Event.RGA -> Unit // No backward on RGAProjection.
    }
    return model
  }
}
