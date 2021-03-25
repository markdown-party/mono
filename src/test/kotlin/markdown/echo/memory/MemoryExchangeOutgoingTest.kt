package markdown.echo.memory

import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.runBlocking
import markdown.echo.Message.V1.Incoming as I
import markdown.echo.Message.V1.Outgoing as O
import markdown.echo.causal.SiteIdentifier
import markdown.echo.channelLink
import markdown.echo.mutableSite
import markdown.echo.sync
import org.junit.jupiter.api.Test

@OptIn(
    ExperimentalCoroutinesApi::class,
)
class MemoryExchangeOutgoingTest {

  @Test
  fun `Only Done works on buffered outgoing`() = runBlocking {
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
