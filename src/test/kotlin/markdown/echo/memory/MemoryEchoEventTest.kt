package markdown.echo.memory

import kotlinx.coroutines.runBlocking
import markdown.echo.Echo
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.events.event
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
                assertEquals(EventIdentifier(SequenceNumber(0), site), yield(123))
            }
        }
    }

    @Test
    fun `MemoryEcho terminates on multiple event {} yields`() = runBlocking {
        val site = SiteIdentifier(145)
        with(Echo.memory<Int>(site)) {
            event {
                assertEquals(EventIdentifier(SequenceNumber(0), site), yield(123))
                assertEquals(EventIdentifier(SequenceNumber(1), site), yield(456))
            }
        }
    }
}
