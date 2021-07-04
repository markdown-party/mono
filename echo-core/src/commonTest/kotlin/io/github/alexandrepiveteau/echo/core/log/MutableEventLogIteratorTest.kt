package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import kotlin.random.Random
import kotlin.test.*

class MutableEventLogIteratorTest {

  @Test
  fun empty() {
    val log = mutableEventLogOf()
    val iterator = log.iterator()
    assertFalse(iterator.hasPrevious())
    assertFalse(iterator.hasNext())
    assertFails { iterator.seqno }
    assertFails { iterator.moveNext() }
    assertFails { iterator.movePrevious() }
  }

  @Test
  fun one() {
    val site = SiteIdentifier.Min
    val data = byteArrayOf(1, 2, 3)
    val log = mutableEventLogOf().apply { append(site, data) }
    val iterator = log.iterator()
    assertTrue(iterator.hasPrevious())
    assertFalse(iterator.hasNext())
    assertFails { iterator.moveNext() }
    iterator.movePrevious()
    assertFalse(iterator.hasPrevious())
    assertFalse(iterator.hasNext())
    assertEquals(site, iterator.site)
    assertEquals(SequenceNumber.Min, iterator.seqno)
    assertEquals(0, iterator.from)
    assertEquals(3, iterator.until)
    assertContentEquals(data, iterator.event.copyOfRange(iterator.from, iterator.until))
  }

  @Test
  fun two() {
    val site1 = Random.nextSiteIdentifier()
    val site2 = Random.nextSiteIdentifier()
    val data1 = byteArrayOf(0, 1)
    val data2 = byteArrayOf(2, 3, 4)

    val log = mutableEventLogOf()
    val (seq1, _) = log.append(site1, data1)
    val (seq2, _) = log.append(site2, data2)

    val iterator = log.iterator()

    val event2 = iterator.previous()
    assertTrue(iterator.hasPrevious())
    assertFalse(iterator.hasNext())
    assertEquals(site2, event2.site)
    assertEquals(seq2, event2.seqno)
    assertContentEquals(data2, event2.data)

    val event1 = iterator.previous()
    assertFalse(iterator.hasPrevious())
    assertTrue(iterator.hasNext())
    assertEquals(site1, event1.site)
    assertEquals(seq1, event1.seqno)
    assertContentEquals(data1, event1.data)

    val event2bis = iterator.next()
    assertEquals(event2, event2bis)
    assertFalse(iterator.hasNext())
    assertTrue(iterator.hasPrevious())
  }
}
