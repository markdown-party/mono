import androidx.compose.runtime.*
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import kotlin.random.Random
import kotlinext.js.require
import kotlinx.browser.window
import org.jetbrains.compose.web.renderComposable
import party.markdown.*
import party.markdown.data.Configuration
import party.markdown.ui.MarkdownParty

/** Returns the next random session identifier. */
private fun Random.nextSessionIdentifier(): String = buildString {
  repeat(32) { append('a' + nextInt('z' - 'a' + 1)) }
}

/**
 * A context that provides the site identifier for the current editor. This site identifier is
 * guaranteed never to change within a single editing session, until the page gets reloaded.
 */
val LocalSiteIdentifier = compositionLocalOf<SiteIdentifier> { error("No SiteIdentifier found.") }

fun main() {

  // Load CSS
  require("./app.css")

  // Creates a new session identifier, if needed.
  val splits = window.location.pathname.split("/")
  var session = splits.getOrNull(1)
  if (session == null || session.isBlank()) {
    session = Random.nextSessionIdentifier()
    window.history.pushState(null, "", "/$session")
  }

  // Create the local and configuration.
  val site = Random.nextSiteIdentifier()
  val config = Configuration.fromWindow(session)

  val local =
      mutableSite<MarkdownParty, MarkdownPartyEvent, MutableMarkdownParty>(
          identifier = site,
          history = MarkdownPartyHistory(),
          transform = MutableMarkdownParty::toMarkdownParty,
      )

  // Generate the information to share the document.
  val publicLink = window.document.URL

  renderComposable("root") {

    // Persistence.
    // LoadEventsEffect(session, local)
    // SaveEventsEffect(session, local)

    // Application.
    CompositionLocalProvider(LocalSiteIdentifier provides site) {
      MarkdownParty(
          link = publicLink,
          configuration = config,
          local = local,
      )
    }
  }
}
