@file:OptIn(ExperimentalCoroutinesApi::class)

package markdown.echo.memory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.runBlocking
import markdown.echo.*
import markdown.echo.causal.SiteIdentifier
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import markdown.echo.Message.V1.Incoming as I
import markdown.echo.Message.V1.Outgoing as O

@OptIn(EchoPreview::class)
class MemoryEchoTest {

    @Test
    fun `Only Done works on buffered incoming`() = runBlocking {
        val echo = Echo.memory<Nothing>(SiteIdentifier(123))
        val exchange = channelExchange<I<Nothing>, O<Nothing>> { incoming ->
            assertTrue(incoming.receive() is I.Ready)
            send(O.Done)
            assertTrue(incoming.receive() is I.Done)
            assertNull(incoming.receiveOrNull())
        }
        sync(echo.incoming(), exchange)
    }

    @Ignore
    @Test
    fun `Only Done works on rendezvous incoming`() = runBlocking {
        val echo = Echo.memory<Nothing>(SiteIdentifier(123)).buffer(Channel.RENDEZVOUS)
        val exchange = channelExchange<I<Nothing>, O<Nothing>> { incoming ->
            send(O.Done)
            val msg = incoming.receive()
            println(msg)
            //assertTrue(msg is I.Done)
            assertNull(incoming.receiveOrNull())
        }.buffer(Channel.RENDEZVOUS)
        sync(echo.incoming(), exchange)
    }
}
