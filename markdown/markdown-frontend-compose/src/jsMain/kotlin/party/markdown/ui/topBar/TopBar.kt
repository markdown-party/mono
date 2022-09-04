package party.markdown.ui.topBar

import LocalSiteIdentifier
import androidx.compose.runtime.Composable
import io.github.alexandrepiveteau.echo.MutableSite
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*
import party.markdown.MarkdownParty
import party.markdown.MarkdownPartyEvent
import party.markdown.data.Configuration
import party.markdown.data.project.ProjectApi
import party.markdown.ui.editor.colorToTailwind
import party.markdown.ui.editor.toCursor

/**
 * @param publicLink the public link that should be used to access the document from a separate
 * browser.
 * @param local the local [MutableSite], aka the local source of truth on which the operations are
 * performed.
 * @param configuration the [Configuration], with which we should handle signaling.
 * @param projectApi the [ProjectApi] that can be used to manage the current project.
 * @param debugEnabled `true` if the debug options should be displayed.
 * @param onDebugEnabled a callback called whenever we should change the debug state.
 */
@Composable
fun TopBar(
    publicLink: String,
    local: MutableSite<MarkdownPartyEvent, MarkdownParty>,
    configuration: Configuration,
    projectApi: ProjectApi,
    debugEnabled: Boolean,
    onDebugEnabled: (Boolean) -> Unit,
) {
  val (icon, color) = LocalSiteIdentifier.current.toCursor()
  Div(
      attrs = {
        classes("p-4", "space-x-4", "bg-gray-800", "text-white", "flex", "flex-row", "items-center")
      }) {
    Img(
        src = "/img/logo.svg",
        attrs = {
          classes("h-12", "cursor-pointer", "hover:bg-gray-600", "rounded", "px-2", "py-1")
          onClick { onDebugEnabled(!debugEnabled) }
        })
    A(
      href = "https://github.com/markdown-party/mono",
      attrs = {
        classes("hover:bg-gray-600", "rounded", "p-3")
      }
    ) {
      Img(src = "/icons/github.svg")
    }
    ProjectName(api = projectApi)
    Div(attrs = { classes("flex-grow") })
    Button(
        onClick = { window.open(window.location.origin) },
    ) {
      Img(src = "/icons/new-project.svg")
      Span { Text("New project") }
    }
    ShareLinkButton(publicLink)
    SyncIndicator(
        local = local,
        configuration = configuration,
        debugMode = debugEnabled,
    )
    Player(
        icon = icon,
        color = colorToTailwind(color),
    )
  }
}
