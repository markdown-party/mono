package markdown.echo.memory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.runBlocking
import markdown.echo.Echo
import markdown.echo.EchoSyncPreview
import markdown.echo.causal.SiteIdentifier
import markdown.echo.channelExchange
import markdown.echo.sync
import org.junit.jupiter.api.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import markdown.echo.Message.V1.Incoming as I
import markdown.echo.Message.V1.Outgoing as O

@OptIn(
    EchoSyncPreview::class,
    ExperimentalCoroutinesApi::class,
)
class MemoryEchoOutgoingTest {

    @Test
    fun `Only Done works on buffered outgoing`() = runBlocking {
        val echo = Echo.memory<Nothing>(SiteIdentifier(123))
        val exchange = channelExchange<O<Nothing>, I<Nothing>> { incoming ->
            send(I.Done)
            assertTrue(incoming.receive() is O.Done)
            assertNull(incoming.receiveOrNull())
        }
        sync(echo.outgoing(), exchange)
    }
}
