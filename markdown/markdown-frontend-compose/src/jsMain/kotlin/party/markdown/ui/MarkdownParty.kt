package party.markdown.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.protocol.Message
import org.jetbrains.compose.web.dom.Div
import party.markdown.MarkdownParty
import party.markdown.MarkdownPartyEvent
import party.markdown.data.Configuration
import party.markdown.data.project.MutableSiteProjectApi
import party.markdown.data.project.ProjectApi
import party.markdown.data.text.MutableSiteTextApi
import party.markdown.data.text.TextApi
import party.markdown.data.tree.MutableSiteTreeApi
import party.markdown.data.tree.TreeApi
import party.markdown.ui.topBar.TopBar

@Composable
fun MarkdownParty(
    link: String,
    local: MutableSite<MarkdownPartyEvent, MarkdownParty>,
    configuration: Configuration,
) {
  val projectApi = remember<ProjectApi>(local) { MutableSiteProjectApi(local) }
  val treeApi = remember<TreeApi>(local) { MutableSiteTreeApi(local) }
  val textApi = remember<TextApi>(local) { MutableSiteTextApi(local) }
  val (debug, setDebug) = remember { mutableStateOf(false) }

  Div(attrs = { classes("flex", "flex-col", "h-screen", "w-screen") }) {
    TopBar(
        publicLink = link,
        local = local,
        configuration = configuration,
        projectApi = projectApi,
        debugEnabled = debug,
        onDebugEnabled = setDebug,
    )
    DividerVertical()
    Panes(
        treeApi = treeApi,
        textApi = textApi,
    )
  }
}
