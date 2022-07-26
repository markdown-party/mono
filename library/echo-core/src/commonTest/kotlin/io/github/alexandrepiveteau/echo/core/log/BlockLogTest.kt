package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.assertThrows
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockLogTest {

  @Test
  fun emptyBlockLog_hasEmptyIterator() {
    val log = BlockLog()
    assertTrue(log.iterator().isEmpty())
    assertTrue(log.iteratorAtEnd().isEmpty())
  }

  @Test
  fun concurrentIterators_throwException() {
    val log = BlockLog()
    val a = log.iterator()
    log.iterator()
    assertThrows<ConcurrentModificationException> { a.hasNext() }
  }

  @Test
  fun adding_removing_isEmpty() {
    val log = BlockLog()
    val iterator = log.iterator()
    iterator.add(SequenceNumber.Min, SiteIdentifier.Min, byteArrayOf(1, 2))
    iterator.movePrevious()
    iterator.remove()
    assertEquals(0, log.size)
    assertTrue(iterator.isEmpty())
  }
}
