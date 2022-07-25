package party.markdown.data.text

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import io.github.alexandrepiveteau.echo.core.causality.eventIdentifierArrayOf
import io.github.alexandrepiveteau.echo.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import party.markdown.MarkdownParty
import party.markdown.MarkdownPartyEvent
import party.markdown.cursors.CursorEvent
import party.markdown.cursors.Cursors
import party.markdown.rga.RGAEvent

private val DefaultDocument = charArrayOf() to eventIdentifierArrayOf()

/** An implementation of the [TextApi] backed by a [MutableSite]. */
class MutableSiteTextApi(
    private val site: MutableSite<MarkdownPartyEvent, MarkdownParty>,
) : TextApi {

  override fun current(
      id: EventIdentifier,
  ): Flow<Triple<CharArray, EventIdentifierArray, Set<Cursors.Cursor>>> =
      site.value.map {
        val (char, event) = it.documents[id] ?: DefaultDocument
        val cursors = it.cursors[id]
        Triple(char, event, cursors)
      }

  override suspend fun edit(
      id: EventIdentifier,
      scope: suspend TextCursorEventScope.() -> Unit,
  ) =
      site.event {
        val impl =
            object : TextCursorEventScope {

              override fun yield(
                  event: RGAEvent,
              ) = yield(MarkdownPartyEvent.RGA(id, event))

              override fun yield(
                  event: CursorEvent,
              ): EventIdentifier = yield(MarkdownPartyEvent.Cursor(event))
            }
        scope(impl)
      }
}
