package io.github.alexandrepiveteau.echo.causal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class EventIdentifierTest {

  @Test
  fun `single site respects ordering`() {
    val site = SiteIdentifier(1)

    val a = EventIdentifier(SequenceNumber(1U), site)
    val b = EventIdentifier(SequenceNumber(2U), site)

    assertTrue(a < b)
    assertEquals(a, a)
    assertEquals(b, b)
  }

  @Test
  fun `multiple sites respect sequence number ordering`() {
    val alice = SiteIdentifier(1)
    val bob = SiteIdentifier(2)

    val a = EventIdentifier(SequenceNumber(0U), alice)
    val b = EventIdentifier(SequenceNumber(0U), bob)
    val c = EventIdentifier(SequenceNumber(1U), alice)
    val d = EventIdentifier(SequenceNumber(1U), bob)

    assertTrue(a < d)
    assertTrue(b < c)
  }

  @Test
  fun `concurrent updates define a total ordering`() {
    val alice = SiteIdentifier(1)
    val bob = SiteIdentifier(2)
    val charlie = SiteIdentifier(3)

    val a = EventIdentifier(SequenceNumber.Zero, alice)
    val b = EventIdentifier(SequenceNumber.Zero, bob)
    val c = EventIdentifier(SequenceNumber.Zero, charlie)

    assertTrue(a < b)
    assertTrue(b < c)
    assertTrue(b < c)
  }
}
