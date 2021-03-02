package markdown.echo.memory

import kotlinx.coroutines.runBlocking
import markdown.echo.Echo
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
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
}
