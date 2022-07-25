package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.copyOfRange
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlin.test.*

class MutableEventLogRemoveTest {

  @Test
  fun removeEmpty() {
    val log = mutableEventLogOf()
    assertFalse(log.remove(SequenceNumber.Min, SiteIdentifier.Min))
    assertFalse(log.remove(SequenceNumber.Max, SiteIdentifier.Max))
  }

  @Test
  fun removeMissing() {
    val log = mutableEventLogOf()
    log.insert(SequenceNumber.Min, SiteIdentifier.Min, byteArrayOf())
    assertFalse(log.remove(SequenceNumber.Min, SiteIdentifier.Max))
    assertEquals(1, log.size)
  }

  @Test
  fun removeTwice() {
    val log = mutableEventLogOf()
    log.insert(SequenceNumber.Min + 0u, SiteIdentifier.Min, byteArrayOf(1, 2))
    log.insert(SequenceNumber.Min + 1u, SiteIdentifier.Min, byteArrayOf(3, 4))
    assertTrue(log.remove(SequenceNumber.Min + 0u, SiteIdentifier.Min))
    log.remove(SequenceNumber.Min + 0u, SiteIdentifier.Min)
    assertEquals(1, log.size)
  }

  @Test
  fun removeTwice_bis() {
    val log = mutableEventLogOf()
    log.insert(SequenceNumber.Min + 0u, SiteIdentifier.Min, byteArrayOf(1, 2))
    log.insert(SequenceNumber.Min + 1u, SiteIdentifier.Min, byteArrayOf(3, 4))
    assertTrue(log.remove(SequenceNumber.Min + 1u, SiteIdentifier.Min))
    assertFalse(log.remove(SequenceNumber.Min + 1u, SiteIdentifier.Min))
    assertEquals(1, log.size)
  }

  @Test
  fun removeOne() {
    val log = mutableEventLogOf()
    log.insert(SequenceNumber.Max, SiteIdentifier.Min, byteArrayOf(1, 2, 3))
    assertTrue(log.remove(SequenceNumber.Max, SiteIdentifier.Min))
    assertTrue(log.isEmpty())
  }

  @Test
  fun removeOneBetweenTwo() {
    val log = mutableEventLogOf()
    log.insert(SequenceNumber.Min + 0u, SiteIdentifier.Min, byteArrayOf(1, 2))
    log.insert(SequenceNumber.Min + 1u, SiteIdentifier.Min, byteArrayOf(3, 4, 5))
    log.insert(SequenceNumber.Min + 2u, SiteIdentifier.Min, byteArrayOf(6, 7, 8, 9))
    assertTrue(log.remove(SequenceNumber.Min + 1u, SiteIdentifier.Min))
    assertEquals(2, log.size)

    fun checkIterator(iterator: EventIterator) {
      iterator.movePrevious()
      assertContentEquals(
          byteArrayOf(6, 7, 8, 9),
          iterator.nextEvent.copyOfRange(iterator.nextFrom, iterator.nextUntil),
      )
      assertEquals(SequenceNumber.Min + 2u, iterator.nextSeqno)
      assertTrue(iterator.hasPrevious())
      iterator.movePrevious()
      assertContentEquals(
          byteArrayOf(1, 2),
          iterator.nextEvent.copyOfRange(iterator.nextFrom, iterator.nextUntil),
      )
      assertEquals(SequenceNumber.Min + 0u, iterator.nextSeqno)
      assertFalse(iterator.hasPrevious())
    }

    checkIterator(log.iterator())
    checkIterator(log.iterator(SiteIdentifier.Min))
  }

  @Test
  fun removeOneAtStart() {
    val log = mutableEventLogOf()
    log.insert(SequenceNumber.Min + 0u, SiteIdentifier.Min, byteArrayOf(1, 2))
    log.insert(SequenceNumber.Min + 1u, SiteIdentifier.Min, byteArrayOf(3, 4, 5))
    log.insert(SequenceNumber.Min + 2u, SiteIdentifier.Min, byteArrayOf(6, 7, 8, 9))
    assertTrue(log.remove(SequenceNumber.Min + 0u, SiteIdentifier.Min))
    assertEquals(2, log.size)

    fun checkIterator(iterator: EventIterator) {
      iterator.movePrevious()
      assertContentEquals(
          byteArrayOf(6, 7, 8, 9),
          iterator.nextEvent.copyOfRange(iterator.nextFrom, iterator.nextUntil),
      )
      assertEquals(SequenceNumber.Min + 2u, iterator.nextSeqno)
      assertTrue(iterator.hasPrevious())
      iterator.movePrevious()
      assertContentEquals(
          byteArrayOf(3, 4, 5),
          iterator.nextEvent.copyOfRange(iterator.nextFrom, iterator.nextUntil),
      )
      assertEquals(SequenceNumber.Min + 1u, iterator.nextSeqno)
      assertFalse(iterator.hasPrevious())
    }

    checkIterator(log.iterator())
    checkIterator(log.iterator(SiteIdentifier.Min))
  }

  @Test
  fun removeOneAtEnd() {
    val log = mutableEventLogOf()
    log.insert(SequenceNumber.Min + 0u, SiteIdentifier.Min, byteArrayOf(1, 2))
    log.insert(SequenceNumber.Min + 1u, SiteIdentifier.Min, byteArrayOf(3, 4, 5))
    log.insert(SequenceNumber.Min + 2u, SiteIdentifier.Min, byteArrayOf(6, 7, 8, 9))
    assertTrue(log.remove(SequenceNumber.Min + 2u, SiteIdentifier.Min))
    assertEquals(2, log.size)

    fun checkIterator(iterator: EventIterator) {
      iterator.movePrevious()
      assertContentEquals(
          byteArrayOf(3, 4, 5),
          iterator.nextEvent.copyOfRange(iterator.nextFrom, iterator.nextUntil),
      )
      assertEquals(SequenceNumber.Min + 1u, iterator.nextSeqno)
      assertTrue(iterator.hasPrevious())
      iterator.movePrevious()
      assertContentEquals(
          byteArrayOf(1, 2),
          iterator.nextEvent.copyOfRange(iterator.nextFrom, iterator.nextUntil),
      )
      assertEquals(SequenceNumber.Min + 0u, iterator.nextSeqno)
      assertFalse(iterator.hasPrevious())
    }

    checkIterator(log.iterator())
    checkIterator(log.iterator(SiteIdentifier.Min))
  }
}
