package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlin.test.*

class MutableEventLogInsertTest {

  @Test
  fun empty() {
    val log = mutableEventLogOf()
    log.insert(SequenceNumber.Min, SiteIdentifier.Min, byteArrayOf())
    assertEquals(1, log.size)
    val iterator = log.iterator()
    val event = iterator.next()
    assertTrue(iterator.hasPrevious())
    assertEquals(SequenceNumber.Min, event.seqno)
    assertEquals(SiteIdentifier.Min, event.site)
    assertContentEquals(byteArrayOf(), event.data)
  }

  @Test
  fun multiple_insertions() {
    val log = mutableEventLogOf()
    log.insert(SequenceNumber.Min, SiteIdentifier.Min, byteArrayOf(1))
    log.insert(SequenceNumber.Max, SiteIdentifier.Max, byteArrayOf(2, 3))
    assertEquals(2, log.size)
    val iterator = log.iterator()
    assertEquals(iterator.next(), Event(SequenceNumber.Min, SiteIdentifier.Min, byteArrayOf(1)))
    assertEquals(iterator.next(), Event(SequenceNumber.Max, SiteIdentifier.Max, byteArrayOf(2, 3)))
    assertFalse(iterator.hasNext())
  }
}
