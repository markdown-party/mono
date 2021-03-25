package markdown.echo.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.logs.mutableEventLogOf
import markdown.echo.mutableSite

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

  @Test
  fun `MemoryExchange creates events in empty MutableLog on event {} with one yield`() =
      runBlocking {
    val log = mutableEventLogOf<Int>()
    val site = SiteIdentifier.random()
    val echo = mutableSite(site, log)
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
  fun `MemoryExchange creates events with good ordering on multiple event {} calls`() =
      runBlocking {
    val alice = SiteIdentifier(0)
    val log = mutableEventLogOf<Int>()
    val site = mutableSite(alice, log)

    site.event { yield(123) }
    site.event { yield(456) }

    assertEquals(123, log[SequenceNumber(0U), alice])
    assertEquals(456, log[SequenceNumber(1U), alice])
    assertEquals(setOf(alice), log.sites)
  }
}
