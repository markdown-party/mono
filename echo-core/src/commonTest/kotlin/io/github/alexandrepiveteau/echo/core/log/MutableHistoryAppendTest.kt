package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class MutableHistoryAppendTest {

  @Test
  fun eventLog_emptyEvent() {
    val log = mutableEventLogOf()
    val id = log.append(SiteIdentifier.Min, byteArrayOf())
    assertEquals(1, log.size)
    assertEquals(EventIdentifier(SequenceNumber.Min, SiteIdentifier.Min), id)
  }

  @Test
  fun history_emptyEvent() {
    val log = mutableHistoryOf(Unit, DuplicateEventProjection)
    val id = log.append(SiteIdentifier.Min, byteArrayOf())
    assertEquals(1, log.size)
    assertEquals(EventIdentifier(SequenceNumber.Min, SiteIdentifier.Min), id)
  }

  @Test
  fun eventLog_multiple_emptyEvent() {
    val log = mutableEventLogOf()
    val id1 = log.append(SiteIdentifier.Min, byteArrayOf())
    val id2 = log.append(SiteIdentifier.Min, byteArrayOf())
    val id3 = log.append(SiteIdentifier.Min, byteArrayOf())
    assertEquals(3, log.size)
    assertEquals(EventIdentifier(SequenceNumber.Min + 0u, SiteIdentifier.Min), id1)
    assertEquals(EventIdentifier(SequenceNumber.Min + 1u, SiteIdentifier.Min), id2)
    assertEquals(EventIdentifier(SequenceNumber.Min + 2u, SiteIdentifier.Min), id3)
  }

  @Test
  fun eventLog_multipleEvent() {
    val log = mutableEventLogOf()
    log.append(SiteIdentifier.Min, byteArrayOf(1))
    log.append(SiteIdentifier.Min, byteArrayOf(2, 3))
    log.append(SiteIdentifier.Min, byteArrayOf(3, 4, 5))
    assertEquals(3, log.size)
    assertTrue(log.contains(SequenceNumber.Min + 2u, SiteIdentifier.Min))
  }

  @Test
  fun eventLog_unspecifiedSite() {
    val log = mutableEventLogOf()
    assertFails { log.append(SiteIdentifier.Unspecified, byteArrayOf()) }
  }
}
