package io.github.alexandrepiveteau.markdown.backend

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.coding
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.protocol.Message
import io.github.alexandrepiveteau.echo.protocol.coder

fun main() {
  val alice = mutableSite<Int>(SiteIdentifier.random())
  val transport = alice.coding(
      incoming = Message.V1.Incoming.coder(),
      outgoing = Message.V1.Outgoing.coder(),
  )
  println("Hello world !")
}
