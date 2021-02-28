@file:OptIn(ExperimentalCoroutinesApi::class)

package markdown.echo.memory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.runBlocking
import markdown.echo.Echo
import markdown.echo.EchoPreview
import markdown.echo.causal.SiteIdentifier
import markdown.echo.channelExchange
import markdown.echo.sync
import kotlin.test.Test
import kotlin.test.assertNull
import markdown.echo.Message.V1.Incoming as I
import markdown.echo.Message.V1.Outgoing as O

@OptIn(EchoPreview::class)
class MemoryEchoTest {

    @Test
    fun `Only Done works on buffered incoming`() = runBlocking {
        val echo = Echo.memory<Nothing>(SiteIdentifier(123))
        val exchange = channelExchange<I<Nothing>, O<Nothing>> { incoming ->
            send(O.Done)
            // Drain the queue.
            while (true) {
                val done = incoming.receive() is I.Done
                if (done) break
            }
            assertNull(incoming.receiveOrNull())
        }
        sync(echo.incoming(), exchange)
    }
}
