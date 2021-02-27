@file:OptIn(ExperimentalCoroutinesApi::class)

package markdown.echo.memory.events

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.channelExchange
import markdown.echo.events.SiteSendEcho
import markdown.echo.events.event
import kotlin.test.Test
import kotlin.test.assertEquals
import markdown.echo.Message.V1.Incoming as I
import markdown.echo.Message.V1.Outgoing as O

class EventTest {

    @Test
    fun `Empty event scope generates appropriate messages`() = runBlocking {
        val site = SiteIdentifier(123)
        val echo = object : SiteSendEcho<I<Nothing>, O<Nothing>> {
            override val site = site
            override fun outgoing() = channelExchange<I<Nothing>, O<Nothing>> { incoming ->
                val adv = incoming.receive() as I.Advertisement
                assertEquals(site, adv.site)
                incoming.receive() as I.Ready
                send(O.Request(site = site, seqno = SequenceNumber.Zero, count = 1))
                incoming.receive() as I.Done
                send(O.Done)
            }
        }
        echo.event { }
    }

    @Test
    fun `Empty event skipping Done reception generates appropriate messages`() = runBlocking {
        val site = SiteIdentifier(123)
        val echo = object : SiteSendEcho<I<Nothing>, O<Nothing>> {
            override val site = site
            override fun outgoing() = channelExchange<I<Nothing>, O<Nothing>> { incoming ->
                val adv = incoming.receive() as I.Advertisement
                assertEquals(site, adv.site)
                // Skip waiting for Ready, and instead directly issue a request.
                send(O.Request(site = site, seqno = SequenceNumber.Zero, count = 1))
                incoming.receive() as I.Ready
                incoming.receive() as I.Done
                send(O.Done)
            }
        }
        echo.event { }
    }

    @Test
    fun `Single event generates appropriate messages`() = runBlocking {
        val site = SiteIdentifier(123)
        val body = 1000
        val expected = SequenceNumber(42)
        val echo = object : SiteSendEcho<I<Int>, O<Int>> {
            override val site = site
            override fun outgoing() = channelExchange<I<Int>, O<Int>> { incoming ->
                val adv = incoming.receive() as I.Advertisement
                assertEquals(site, adv.site)
                incoming.receive() as I.Ready
                send(O.Request(expected, site, count = 1))
                val event = incoming.receive() as I.Event
                assertEquals(body, event.body)
                assertEquals(expected, event.seqno)
                assertEquals(site, event.site)
                incoming.receive() as I.Done
                send(O.Done)
            }
        }
        echo.event { yield(body) }
    }

    // TODO : Test emission of multiple messages.
    // TODO : Test emission of Requests with count = 0
    // TODO : Test emission of Requests for different sites.
}
