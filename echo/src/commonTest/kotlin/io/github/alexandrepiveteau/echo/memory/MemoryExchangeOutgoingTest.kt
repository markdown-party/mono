package io.github.alexandrepiveteau.echo.memory

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.channelLink
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.protocol.Message.V1.Incoming as I
import io.github.alexandrepiveteau.echo.protocol.Message.V1.Outgoing as O
import io.github.alexandrepiveteau.echo.suspendTest
import io.github.alexandrepiveteau.echo.sync
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.channels.receiveOrNull

class MemoryExchangeOutgoingTest {

  @Test
  fun onlyDoneWorksOnBufferedOutgoing() = suspendTest {
    val echo = mutableSite<Nothing>(SiteIdentifier(123))
    val exchange =
        channelLink<O<Nothing>, I<Nothing>> { incoming ->
          send(I.Done)
          assertTrue(incoming.receive() is O.Done)
          assertNull(incoming.receiveOrNull())
        }
    sync(echo.outgoing(), exchange)
  }
}
