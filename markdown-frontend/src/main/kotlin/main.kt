@file:OptIn(EchoKtorPreview::class)

import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.ktor.EchoKtorPreview
import io.github.alexandrepiveteau.echo.mutableSite
import kotlin.random.Random
import kotlinx.browser.document
import party.markdown.data.Configuration
import party.markdown.data.toExchange
import party.markdown.tree.MutableTree
import party.markdown.tree.TreeProjection
import party.markdown.ui.markdownParty
import react.dom.render

private val State =
    mutableSite(
        identifier = Random.nextSiteIdentifier(),
        initial = MutableTree(),
        projection = TreeProjection,
        transform = MutableTree::toTree,
    )

fun main() {
  render(document.getElementById("root")) {
    markdownParty {
      local = State
      remote = Configuration.Default.toExchange()
    }
  }
}
