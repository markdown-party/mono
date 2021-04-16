package io.github.alexandrepiveteau.echo.log

import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SequenceNumber.Companion.Zero
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier.Companion.random
import io.github.alexandrepiveteau.echo.logs.persistentEventLogOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentEventLogIteratorTest {

  @Test
  fun `test empty log has empty iterators`() {
    val log = persistentEventLogOf<Unit>()

    assertFalse(log.iterator().hasNext())
    assertFalse(log.eventIterator().hasNext())
    assertFalse(log.eventIterator().hasPrevious())
    assertFalse(log.eventIterator(random(), Zero).hasNext())
    assertFalse(log.eventIterator(random(), Zero).hasPrevious())
  }

  @Test
  fun `test non empty log is read with iterator`() {
    val id = EventIdentifier(SequenceNumber(4u), SiteIdentifier(4))
    val body = "Hello"
    val log = persistentEventLogOf(id to body)

    val iterator = log.iterator()
    // val eventIteratorFromSelf = log.eventIterator(id.site, id.seqno)

    // iterator
    assertTrue(iterator.hasNext())
    assertEquals(body, iterator.next().body)
    assertFalse(iterator.hasNext())
  }

  @Test
  fun `test non empty log is read with eventIterator and no start`() {
    val id = EventIdentifier(SequenceNumber(4u), SiteIdentifier(4))
    val body = "Hello"
    val log = persistentEventLogOf(id to body)
    val eventIteratorAll = log.eventIterator()

    // eventIteratorAll
    assertTrue(eventIteratorAll.hasNext())
    assertFalse(eventIteratorAll.hasPrevious())
    assertEquals(id, eventIteratorAll.nextIndex())
    assertEquals(body, eventIteratorAll.next().body)
    assertTrue(eventIteratorAll.hasPrevious())
    assertFalse(eventIteratorAll.hasNext())

    assertEquals(id, log.eventIterator().next().identifier)
  }

  @Test
  fun `test non empty log is read with eventIteratorStartingFromZero`() {
    val id = EventIdentifier(SequenceNumber(4u), SiteIdentifier(4))
    val body = "Hello"
    val log = persistentEventLogOf(id to body)
    val eventIteratorFromStart = log.eventIterator(SiteIdentifier(123), Zero)

    // eventIteratorFromStart
    assertTrue(eventIteratorFromStart.hasNext())
    assertFalse(eventIteratorFromStart.hasPrevious())
    assertEquals(id, eventIteratorFromStart.nextIndex())
    assertEquals(body, eventIteratorFromStart.next().body)
    assertTrue(eventIteratorFromStart.hasPrevious())
    assertFalse(eventIteratorFromStart.hasNext())

    assertEquals(id, log.eventIterator(SiteIdentifier(123), Zero).next().identifier)
  }

  @Test
  fun `test non empty log is read with eventIteratorStartingFromEnd`() {
    val id = EventIdentifier(SequenceNumber(4u), SiteIdentifier(4))
    val body = "Hello"
    val log = persistentEventLogOf(id to body)
    val eventIteratorFromEnd = log.eventIterator(SiteIdentifier(123), SequenceNumber(10u))

    // eventIteratorFromStart
    assertFalse(eventIteratorFromEnd.hasNext())
    assertTrue(eventIteratorFromEnd.hasPrevious())
    assertEquals(id, eventIteratorFromEnd.previousIndex())
    assertEquals(body, eventIteratorFromEnd.previous().body)
    assertTrue(eventIteratorFromEnd.hasNext())
    assertFalse(eventIteratorFromEnd.hasPrevious())

    assertEquals(
        id,
        log.eventIterator(SiteIdentifier(123), SequenceNumber(10u)).previous().identifier,
    )
  }

  @Test
  fun `test non empty log is read from self`() {
    val id = EventIdentifier(SequenceNumber(4u), SiteIdentifier(4))
    val body = "Hello"
    val log = persistentEventLogOf(id to body)
    val eventIteratorFromSelf = log.eventIterator(id.site, id.seqno)

    // eventIteratorFromStart
    assertTrue(eventIteratorFromSelf.hasNext())
    assertFalse(eventIteratorFromSelf.hasPrevious())
    assertEquals(id, eventIteratorFromSelf.nextIndex())
    assertEquals(body, eventIteratorFromSelf.next().body)
    assertTrue(eventIteratorFromSelf.hasPrevious())
    assertFalse(eventIteratorFromSelf.hasNext())

    assertEquals(id, log.eventIterator(id.site, id.seqno).next().identifier)
  }
}
