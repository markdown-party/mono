package io.github.alexandrepiveteau.echo.memory

import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryExchangeEventTest {

  @Test
  fun mutableSite_emptyEvent_terminates() = suspendTest {
    val alice = mutableSite<Int>(SiteIdentifier(123))
    alice.event {}
  }

  @Test
  fun mutableSite_manyEmptyEvent_terminates() = suspendTest {
    val echo = mutableSite<Int>(SiteIdentifier(456))
    repeat(1000) { echo.event {} }
  }

  @Test
  fun mutableSite_manyNonEmptyEvent_terminates() = suspendTest {
    val site = mutableSite<Int>(SiteIdentifier(456))
    val count = 1000
    repeat(count) { iteration -> site.event { yield(iteration) } }
  }

  @Test
  fun mutableSite_singleYield_terminates() = suspendTest {
    val site = SiteIdentifier(145)
    with(mutableSite<Int>(site)) {
      event { assertEquals(EventIdentifier(SequenceNumber(0U), site), yield(123)) }
    }
  }

  @Test
  fun mutableSite_manyYield_terminates() = suspendTest {
    val site = SiteIdentifier(145)
    with(mutableSite<Int>(site)) {
      event {
        assertEquals(EventIdentifier(SequenceNumber(0U), site), yield(123))
        assertEquals(EventIdentifier(SequenceNumber(1U), site), yield(456))
      }
    }
  }
}
