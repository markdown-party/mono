@file:OptIn(ExperimentalCoroutinesApi::class)

package markdown.echo.memory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import markdown.echo.*
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.memory.log.mutableEventLogOf
import kotlin.test.*
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

    @Test
    fun `No messages to incoming works`() = runBlocking {
        val echo = Echo.memory<Nothing>(SiteIdentifier(123)).buffer(RENDEZVOUS)
        val received = echo.incoming().talk(emptyFlow()).toList()
        assertEquals(listOf(I.Ready, I.Done), received)
    }

    @Test
    fun `Only Done works on 1-buffer incoming`() = runBlocking {
        val echo = Echo.memory<Nothing>(SiteIdentifier(123)).buffer(RENDEZVOUS)
        val exchange = channelExchange<I<Nothing>, O<Nothing>> { incoming ->
            assertTrue(incoming.receive() is I.Ready)
            send(O.Done)
            assertTrue(incoming.receive() is I.Done)
            assertNull(incoming.receiveOrNull())
        }.buffer(RENDEZVOUS)
        sync(echo.incoming(), exchange)
    }

    @Test
    fun `Advertises one event and cancels if rendezvous and not empty`() = runBlocking {
        val seqno = SequenceNumber(123)
        val site = SiteIdentifier(456)
        val log = mutableEventLogOf(EventIdentifier(seqno, site) to 42)
        val echo = Echo.memory(site, log).buffer(RENDEZVOUS)
        val exchange = channelExchange<I<Int>, O<Int>> { incoming ->
            assertEquals(I.Advertisement(site), incoming.receive())
            assertEquals(I.Ready, incoming.receive())
            send(O.Done)
            assertTrue(incoming.receive() is I.Done)
            assertNull(incoming.receiveOrNull())
        }.buffer(RENDEZVOUS)
        sync(echo.incoming(), exchange)
    }

    @Test
    fun `Advertises all sites in incoming`() = runBlocking {
        val count = 100
        val sites = List(count) { SiteIdentifier.random() }
        val seqno = SequenceNumber.Zero
        val events = sites.map { site -> EventIdentifier(seqno, site) to 123 }
        val log = mutableEventLogOf(*events.toTypedArray())
        val echo = Echo.memory(SiteIdentifier.random(), log).buffer(RENDEZVOUS)
        val exchange = channelExchange<I<Int>, O<Int>> { incoming ->
            val received = mutableListOf<SiteIdentifier>()
            while (true) {
                when (val msg = incoming.receive()) {
                    is I.Advertisement -> received.add(msg.site)
                    is I.Ready -> break
                    else -> fail("Unexpected message $msg.")
                }
            }
            assertTrue(received.containsAll(sites))
            send(O.Done)
            incoming.receive() as I.Done
            incoming.receiveOrNull()
        }.buffer(RENDEZVOUS)
        sync(echo.incoming(), exchange)
    }

    @Test
    fun `Issues one event on request`() = runBlocking {
        val site = SiteIdentifier(10)
        val seqno = SequenceNumber(150)
        val events = mutableEventLogOf(EventIdentifier(seqno, site) to true)
        val echo = Echo.memory(SiteIdentifier(0), events).buffer(RENDEZVOUS)
        val exchange = channelExchange<I<Boolean>, O<Boolean>> { incoming ->
            assertEquals(I.Advertisement(site), incoming.receive())
            assertEquals(I.Ready, incoming.receive())
            send(O.Request(seqno, site))
            assertEquals(I.Event(seqno, site, true), incoming.receive())
            send(O.Done)
            assertEquals(I.Done, incoming.receive())
            assertNull(incoming.receiveOrNull())
        }.buffer(RENDEZVOUS)
        sync(echo.incoming(), exchange)
    }

    @Test
    fun `No event is sent if request size is zero`() = runBlocking {
        val site = SiteIdentifier(123)
        val seqno = SequenceNumber(150)
        val events = mutableEventLogOf(EventIdentifier(seqno, site) to true)
        val echo = Echo.memory(SiteIdentifier(0), events)
        val exchange = channelExchange<I<Boolean>, O<Boolean>> { incoming ->
            assertEquals(I.Advertisement(site), incoming.receive())
            assertEquals(I.Ready, incoming.receive())
            send(O.Request(seqno, site, count = 0))
            incoming.receive()
            fail("incoming.receive() should have timeout.")
        }
        val nullIfFailure = withTimeoutOrNull(1000) {
            sync(echo.incoming(), exchange)
        }
        assertNull(nullIfFailure)
    }

    @Test
    fun `An event is sent if first request size is zero and second is non-zero`() = runBlocking {
        val site = SiteIdentifier(123)
        val seqno = SequenceNumber(150)
        val events = mutableEventLogOf(EventIdentifier(seqno, site) to true)
        val echo = Echo.memory(SiteIdentifier(0), events)
        val exchange = channelExchange<I<Boolean>, O<Boolean>> { incoming ->
            assertEquals(I.Advertisement(site), incoming.receive())
            assertEquals(I.Ready, incoming.receive())
            send(O.Request(seqno, site, count = 0))
            send(O.Request(seqno, site, count = 1))
            assertEquals(I.Event(seqno, site, true), incoming.receive())
            send(O.Done)
            assertEquals(I.Done, incoming.receive())
            assertNull(incoming.receiveOrNull())
        }
        sync(echo.incoming(), exchange)
    }
}
