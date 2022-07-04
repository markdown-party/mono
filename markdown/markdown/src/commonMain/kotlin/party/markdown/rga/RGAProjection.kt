package party.markdown.rga

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.projections.OneWayProjection

object RGAProjection : OneWayProjection<MutableRGA, RGAEvent> {

  override fun forward(
      model: MutableRGA,
      identifier: EventIdentifier,
      event: RGAEvent,
  ): MutableRGA {
    val (seqno, site) = identifier
    when (event) {
      is RGAEvent.Insert -> model.insert(site, seqno, event.atom, event.offset)
      is RGAEvent.Remove -> model.remove(event.offset)
    }
    return model
  }
}
