@file:OptIn(EchoKtorPreview::class)

import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.ktor.EchoKtorPreview
import io.github.alexandrepiveteau.echo.mutableSite
import kotlin.random.Random
import kotlinx.browser.document
import kotlinx.browser.window
import party.markdown.MarkdownPartyProjection
import party.markdown.MutableMarkdownParty
import party.markdown.data.Configuration
import party.markdown.data.toExchange
import party.markdown.ui.markdownParty
import react.dom.render

/** Returns the next random session identifier. */
@Suppress("UNUSED")
private fun Random.nextSessionIdentifier(): String = buildString {
  repeat(32) { append(('a'..'z').random()) }
}

fun main() {

  // Creates a new session identifier, if needed.
  val splits = window.location.pathname.split("/")
  var sessionOrNull = splits.getOrNull(1)
  if (sessionOrNull == null || sessionOrNull.isBlank()) {
    sessionOrNull = Random.nextSessionIdentifier()
    window.history.pushState(null, "", "/$sessionOrNull")
  }

  // Create the local and remote sites.
  val remote = Configuration.remote(sessionOrNull)
  val local =
      mutableSite(
          Random.nextSiteIdentifier(),
          initial = MutableMarkdownParty(),
          projection = MarkdownPartyProjection,
          transform = MutableMarkdownParty::toMarkdownParty,
      )

  // Generate the information to share the document.
  val publicLink = window.document.URL

  render(document.getElementById("root")) {
    markdownParty {
      this.publicLink = publicLink
      this.remote = remote.toExchange()
      this.local = local
    }
  }
}
