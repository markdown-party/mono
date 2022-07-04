import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import kotlin.random.Random
import kotlinext.js.require
import kotlinx.browser.window
import org.jetbrains.compose.web.renderComposable
import party.markdown.MarkdownParty
import party.markdown.MarkdownPartyEvent
import party.markdown.MarkdownPartyHistory
import party.markdown.MutableMarkdownParty
import party.markdown.data.Configuration
import party.markdown.data.toExchange
import party.markdown.ui.MarkdownParty

/** Returns the next random session identifier. */
@Suppress("UNUSED")
private fun Random.nextSessionIdentifier(): String = buildString {
  repeat(32) { append(('a'..'z').random()) }
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
  var sessionOrNull = splits.getOrNull(1)
  if (sessionOrNull == null || sessionOrNull.isBlank()) {
    sessionOrNull = Random.nextSessionIdentifier()
    window.history.pushState(null, "", "/$sessionOrNull")
  }

  // Create the local and remote sites.
  val site = Random.nextSiteIdentifier()
  val remote = Configuration.remote(sessionOrNull).toExchange()
  val local =
      mutableSite<MarkdownParty, MarkdownPartyEvent, MutableMarkdownParty>(
          identifier = site,
          history = MarkdownPartyHistory(),
          transform = MutableMarkdownParty::toMarkdownParty,
      )

  // Generate the information to share the document.
  val publicLink = window.document.URL

  renderComposable("root") {
    CompositionLocalProvider(LocalSiteIdentifier provides site) {
      MarkdownParty(
          link = publicLink,
          remote = remote,
          local = local,
      )
    }
  }
}
