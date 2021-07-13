package party.markdown.data.text

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import io.github.alexandrepiveteau.echo.core.causality.eventIdentifierArrayOf
import io.github.alexandrepiveteau.echo.events.EventScope
import io.github.alexandrepiveteau.echo.sites.map
import kotlinx.coroutines.flow.StateFlow
import party.markdown.MarkdownParty
import party.markdown.MarkdownPartyEvent
import party.markdown.rga.RGAEvent

private val DefaultDocument = charArrayOf() to eventIdentifierArrayOf()

/** An implementation of the [TextApi] backed by a [MutableSite]. */
class MutableSiteTextApi(
    private val site: MutableSite<MarkdownPartyEvent, MarkdownParty>,
) : TextApi {

  override fun current(
      id: EventIdentifier,
  ): StateFlow<Pair<CharArray, EventIdentifierArray>> =
      site.value.map { it.documents[id] ?: DefaultDocument }

  override suspend fun edit(
      id: EventIdentifier,
      scope: suspend EventScope<RGAEvent>.() -> Unit,
  ) =
      site.event {
        val impl =
            object : EventScope<RGAEvent> {
              override fun yield(
                  event: RGAEvent,
              ) = yield(MarkdownPartyEvent.RGA(id, event))
            }
        scope(impl)
      }
}
