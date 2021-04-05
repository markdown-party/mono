package io.github.alexandrepiveteau.markdown.backend

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.ktor.exchange
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.protocol.decode
import io.github.alexandrepiveteau.echo.sync
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun main(): Unit = coroutineScope {
  val client = HttpClient(CIO)
  val remote =
      client
          .exchange<Int>(
              incoming = {},
              outgoing = {},
          )
          .decode()

  val alice = mutableSite<Int>(SiteIdentifier.random())

  launch { sync(alice, remote) }
}
