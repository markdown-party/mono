package markdown.echo.memory.projections

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import markdown.echo.ReceiveEcho
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.channelExchange
import markdown.echo.projections.projection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import markdown.echo.Message.V1.Incoming as I
import markdown.echo.Message.V1.Outgoing as O

@OptIn(
    ExperimentalCoroutinesApi::class,
)
class OneWayProjectionTest {

    @Test
    fun `Test empty projection returns base value`() = runBlocking {
        val echo = ReceiveEcho {
            channelExchange<O<Int>, I<Int>> { incoming ->
                send(I.Ready)
                send(I.Done)
                assertEquals(O.Done, incoming.receive())
                assertNull(incoming.receiveOrNull())
            }
        }
        val actual = echo.projection(0, CounterOneWayProjection).toList()
        assertEquals(listOf(0), actual)
    }

    @Test
    fun `Test projection with two event returns base and aggregated values`() = runBlocking {
        val site = SiteIdentifier(123)
        val echo = ReceiveEcho {
            channelExchange<O<Int>, I<Int>> { incoming ->
                send(I.Advertisement(site))
                send(I.Ready)
                val request = incoming.receive() as O.Request
                assertEquals(SequenceNumber.Zero, request.nextForAll)
                assertEquals(SequenceNumber.Zero, request.nextForSite)
                assertEquals(site, request.site)
                assertTrue(request.count >= 2)
                send(I.Event(seqno = SequenceNumber.Zero, site = site, body = 5))
                send(I.Event(seqno = SequenceNumber.Zero + 1U, site = site, body = 7))
                send(I.Done)
                assertEquals(O.Done, incoming.receive())
                assertNull(incoming.receiveOrNull())
            }
        }
        val actual = echo.projection(0, CounterOneWayProjection).toList()
        assertEquals(listOf(0, 5, 12), actual)
    }
}
