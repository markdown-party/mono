package io.github.alexandrepiveteau.echo.core

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class EventLogTest {

  @Test
  fun emptyLog_isEmpty() {
    val log = EventLog()
    assertEquals(0, log.size)
    assertEquals(0, log.acknowledged().size)
  }

  @Test
  fun emptyLog_insertOne() {
    val log = EventLog()
    val site = Random.nextSiteIdentifier()
    log.append(ByteArray(100), site)
    val expected = log.acknowledged()

    assertEquals(1, log.size)
    assertEquals(1, expected.size)
    assertEquals(EventIdentifier(SequenceNumber.Min, site), expected[0])
  }

  @Test
  fun nonEmptyLog_clear() {
    val log =
        EventLog().apply {
          append(ByteArray(0), Random.nextSiteIdentifier())
          append(ByteArray(0), Random.nextSiteIdentifier())
        }
    log.clear()
    assertEquals(0, log.size)
  }
}
