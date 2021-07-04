package io.github.alexandrepiveteau.echo.site

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.suspendTest
import io.github.alexandrepiveteau.echo.sync
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.withTimeoutOrNull

class MutableSiteEventTest {

  @Test
  fun empty_event_terminates() = suspendTest {
    val alice = mutableSite<Int>(123U.toSiteIdentifier())
    alice.event {}
  }

  @Test
  fun multiple_empty_event_terminates() = suspendTest {
    val echo = mutableSite<Int>(456U.toSiteIdentifier())
    repeat(2) { echo.event {} }
  }

  @Test
  fun multiple_nonEmpty_event_terminates() = suspendTest {
    val site = mutableSite<Int>(456U.toSiteIdentifier())
    repeat(2) { iteration -> site.event { yield(iteration) } }
  }

  @Test
  fun singleYield_terminates() = suspendTest {
    val site = 145U.toSiteIdentifier()
    with(mutableSite<Int>(site)) {
      event { assertEquals(EventIdentifier(SequenceNumber.Min, site), yield(123)) }
    }
  }

  @Test
  fun multipleYield_terminates() = suspendTest {
    val site = 145U.toSiteIdentifier()
    with(mutableSite<Int>(site)) {
      event {
        assertEquals(EventIdentifier(SequenceNumber.Min + 0u, site), yield(123))
        assertEquals(EventIdentifier(SequenceNumber.Min + 1u, site), yield(456))
      }
    }
  }

  @Test
  fun sequential_yields_areOrdered() = suspendTest {
    val alice = mutableSite<Int>(Random.nextSiteIdentifier())
    val bob = mutableSite<Int>(Random.nextSiteIdentifier())
    alice.event { assertEquals(SequenceNumber.Min + 0u, yield(123).seqno) }
    // TODO : Use one-shot sync when supported.
    withTimeoutOrNull(100) { sync(alice, bob) }
    bob.event { assertEquals(SequenceNumber.Min + 1u, yield(123).seqno) }
  }
}
