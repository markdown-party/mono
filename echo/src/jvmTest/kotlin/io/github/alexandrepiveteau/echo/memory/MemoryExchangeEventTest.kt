package io.github.alexandrepiveteau.echo.memory

import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

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
  fun `MutableSite terminates on plenty non-empty event {} calls`() = runBlocking {
    val site = mutableSite<Int>(SiteIdentifier(456))
    val count = 1000
    val millis = measureTimeMillis {
      repeat(count) { iteration -> site.event { yield(iteration) } }
    }
    println("Took $millis ms.")
  }

  @Test
  fun `MemoryExchange terminates on single event {} yield`() = runBlocking {
    val site = SiteIdentifier(145)
    with(io.github.alexandrepiveteau.echo.mutableSite<Int>(site)) {
      event { assertEquals(EventIdentifier(SequenceNumber(0U), site), yield(123)) }
    }
  }

  @Test
  fun `MemoryExchange terminates on multiple event {} yields`() = runBlocking {
    val site = SiteIdentifier(145)
    with(io.github.alexandrepiveteau.echo.mutableSite<Int>(site)) {
      event {
        assertEquals(EventIdentifier(SequenceNumber(0U), site), yield(123))
        assertEquals(EventIdentifier(SequenceNumber(1U), site), yield(456))
      }
    }
  }
}
