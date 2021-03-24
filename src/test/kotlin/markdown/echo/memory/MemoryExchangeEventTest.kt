package markdown.echo.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import markdown.echo.Exchange
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.events.SiteSendExchange
import markdown.echo.events.event
import markdown.echo.memory.log.mutableEventLogOf

class MemoryExchangeEventTest {

  @Test
  fun `MemoryExchange terminates on empty event {} call`() = runBlocking {
    val echo = Exchange.memory<Int>(SiteIdentifier(123))
    echo.event {}
  }

  @Test
  fun `MemoryExchange terminates on plenty empty event {} calls`() = runBlocking {
    val echo = Exchange.memory<Int>(SiteIdentifier(456))
    repeat(1000) { echo.event {} }
  }

  @Test
  fun `MemoryExchange terminates on single event {} yield`() = runBlocking {
    val site = SiteIdentifier(145)
    with(Exchange.memory<Int>(site)) {
      event { assertEquals(EventIdentifier(SequenceNumber(0U), site), yield(123)) }
    }
  }

  @Test
  fun `MemoryExchange terminates on multiple event {} yields`() = runBlocking {
    val site = SiteIdentifier(145)
    with(Exchange.memory<Int>(site)) {
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
    val echo = Exchange.memory(site, log)
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

    // A small utility that lets multiple sites use the same Echo, without requiring inter-Echo
    // sync.
    fun <A, B> SiteSendExchange<A, B>.with(site: SiteIdentifier): SiteSendExchange<A, B> {
      return object : SiteSendExchange<A, B> by this {
        override val site = site
      }
    }

    val alice = SiteIdentifier(1)
    val bob = SiteIdentifier(2)
    val log = mutableEventLogOf<Int>()
    val echo = Exchange.memory(SiteIdentifier(0), log)

    echo.with(alice).event { yield(123) }
    echo.with(bob).event { yield(456) }
    assertEquals(123, log[SequenceNumber(0U), alice])
    assertEquals(456, log[SequenceNumber(1U), bob])
    assertEquals(setOf(alice, bob), log.sites)
  }
}
