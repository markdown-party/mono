package markdown.echo.memory

import kotlinx.coroutines.runBlocking
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.mutableSite
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryExchangeEventTest {

  @Test
  fun `mutable site terminates on empty event {} call`() = runBlocking {
    val alice = mutableSite<Int>(SiteIdentifier(123))
    alice.event {}
  }

  @Test
  fun `MemoryExchange terminates on plenty empty event {} calls`() = runBlocking {
    val echo = mutableSite<Int>(SiteIdentifier(456))
    repeat(1000) { echo.event {} }
  }

  @Test
  fun `MemoryExchange terminates on single event {} yield`() = runBlocking {
    val site = SiteIdentifier(145)
    with(mutableSite<Int>(site)) {
      event { assertEquals(EventIdentifier(SequenceNumber(0U), site), yield(123)) }
    }
  }

  @Test
  fun `MemoryExchange terminates on multiple event {} yields`() = runBlocking {
    val site = SiteIdentifier(145)
    with(mutableSite<Int>(site)) {
      event {
        assertEquals(EventIdentifier(SequenceNumber(0U), site), yield(123))
        assertEquals(EventIdentifier(SequenceNumber(1U), site), yield(456))
      }
    }
  }
}
