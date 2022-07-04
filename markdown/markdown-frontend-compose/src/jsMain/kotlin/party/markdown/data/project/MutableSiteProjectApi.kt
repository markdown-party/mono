package party.markdown.data.project

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.map
import kotlinx.coroutines.flow.StateFlow
import party.markdown.MarkdownParty
import party.markdown.MarkdownPartyEvent
import party.markdown.tree.TreeEvent
import party.markdown.tree.TreeNodeRoot

private const val DefaultName = "Unnamed project"

/** An implementation of the [ProjectApi] backed by a [MutableSite]. */
class MutableSiteProjectApi(
    private val site: MutableSite<MarkdownPartyEvent, MarkdownParty>,
) : ProjectApi {

  override val currentName: StateFlow<String> = site.map { it.tree.name ?: DefaultName }

  override suspend fun name(
      value: String,
  ): Unit = site.event { yield(MarkdownPartyEvent.Tree(TreeEvent.Name(TreeNodeRoot, value))) }
}
