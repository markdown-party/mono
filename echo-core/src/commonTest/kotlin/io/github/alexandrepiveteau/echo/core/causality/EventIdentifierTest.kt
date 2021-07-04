package io.github.alexandrepiveteau.echo.core.causality

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventIdentifierTest {

  @Test
  fun singleSite_respectsOrdering() {
    val site = SiteIdentifier(1U)

    val a = EventIdentifier(SequenceNumber(1U), site)
    val b = EventIdentifier(SequenceNumber(2U), site)

    assertTrue(a < b)
    assertEquals(a, a)
    assertEquals(b, b)
  }

  @Test
  fun multipleSites_respectSequenceNumberOrdering() {
    val alice = SiteIdentifier(1U)
    val bob = SiteIdentifier(2U)

    val a = EventIdentifier(SequenceNumber(0U), alice)
    val b = EventIdentifier(SequenceNumber(0U), bob)
    val c = EventIdentifier(SequenceNumber(1U), alice)
    val d = EventIdentifier(SequenceNumber(1U), bob)

    assertTrue(a < d)
    assertTrue(b < c)
  }

  @Test
  fun concurrentUpdates_makeTotalOrdering() {
    val alice = SiteIdentifier(1U)
    val bob = SiteIdentifier(2U)
    val charlie = SiteIdentifier(3U)

    val a = EventIdentifier(SequenceNumber.Min, alice)
    val b = EventIdentifier(SequenceNumber.Min, bob)
    val c = EventIdentifier(SequenceNumber.Min, charlie)

    assertTrue(a < b)
    assertTrue(b < c)
    assertTrue(b < c)
  }
}
