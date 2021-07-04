package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MutableEventLogInsertTest {

  @Test
  fun empty() {
    val log = mutableEventLogOf()
    log.insert(SequenceNumber.Min, SiteIdentifier.Min, byteArrayOf())
    assertEquals(1, log.size)
    val iterator = log.iterator()
    val event = iterator.previous()
    assertFalse(iterator.hasPrevious())
    assertEquals(SequenceNumber.Min, event.seqno)
    assertEquals(SiteIdentifier.Min, event.site)
    assertContentEquals(byteArrayOf(), event.data)
  }
}
