package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.assertThrows
import kotlin.test.Test
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
}
