package markdown.echo.memory

import kotlinx.coroutines.runBlocking
import markdown.echo.Echo
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.events.SiteSendEcho
import markdown.echo.events.event
import markdown.echo.memory.log.mutableEventLogOf
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryEchoEventTest {

    @Test
    fun `MemoryEcho terminates on empty event {} call`() = runBlocking {
        val echo = Echo.memory<Int>(SiteIdentifier(123))
        echo.event { }
    }

    @Test
    fun `MemoryEcho terminates on plenty empty event {} calls`() = runBlocking {
        val echo = Echo.memory<Int>(SiteIdentifier(456))
        repeat(1000) { echo.event { } }
    }

    @Test
    fun `MemoryEcho terminates on single event {} yield`() = runBlocking {
        val site = SiteIdentifier(145)
        with(Echo.memory<Int>(site)) {
            event {
                assertEquals(EventIdentifier(SequenceNumber(0U), site), yield(123))
            }
        }
    }

    @Test
    fun `MemoryEcho terminates on multiple event {} yields`() = runBlocking {
        val site = SiteIdentifier(145)
        with(Echo.memory<Int>(site)) {
            event {
                assertEquals(EventIdentifier(SequenceNumber(0U), site), yield(123))
                assertEquals(EventIdentifier(SequenceNumber(1U), site), yield(456))
            }
        }
    }

    @Test
    fun `MemoryEcho creates events in empty MutableLog on event {} with one yield`() = runBlocking {
        val log = mutableEventLogOf<Int>()
        val site = SiteIdentifier.random()
        val echo = Echo.memory(site, log)
        echo.event {
            yield(123)
            yield(456)
        }
        assertEquals(123, log[SequenceNumber(0U), site])
        assertEquals(456, log[SequenceNumber(1U), site])
        assertEquals(SequenceNumber(2U), log.expected(site))
        assertEquals(setOf(site), log.sites)
    }

    @Test
    fun `MemoryEcho creates events with good ordering on multiple event {} calls`() = runBlocking {

        // A small utility that lets multiple sites use the same Echo, without requiring inter-Echo
        // sync.
        fun <A, B> SiteSendEcho<A, B>.with(site: SiteIdentifier): SiteSendEcho<A, B> {
            return object : SiteSendEcho<A, B> by this {
                override val site = site
            }
        }

        val alice = SiteIdentifier(1)
        val bob = SiteIdentifier(2)
        val log = mutableEventLogOf<Int>()
        val echo = Echo.memory(SiteIdentifier(0), log)

        echo.with(alice).event {
            yield(123)
        }
        echo.with(bob).event {
            yield(456)
        }
        assertEquals(123, log[SequenceNumber(0U), alice])
        assertEquals(456, log[SequenceNumber(1U), bob])
        assertEquals(setOf(alice, bob), log.sites)
    }
}
