package io.github.alexandrepiveteau.echo.core

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber.Companion.Min
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

private val Alice = SiteIdentifier(1U)
private val Bob = SiteIdentifier(2U)

class EventLogInsertTest {

  @Test
  fun insertOne() {
    val log = EventLog()
    val identifier = EventIdentifier(Min, Alice)
    log.insert(byteArrayOf(1, 2, 3, 4), identifier.seqno, identifier.site)
    assertEquals(1, log.size)
    assertEquals(1, log.acknowledged().size)
    assertEquals(identifier, log.acknowledged()[0])
  }

  @Test
  fun insertTwo() {
    val log = EventLog()
    log.insert(byteArrayOf(1, 2, 3, 4), Min, Alice)
    log.insert(byteArrayOf(5, 6, 7, 8), Min, Bob)
    assertEquals(2, log.size)
    assertEquals(2, log.acknowledged().size)
    for (i in 0 until 2) {
      assertEquals(Min, log.acknowledged()[i].seqno)
    }
  }

  @Test
  fun insertTwo_outOfOrder() {
    val log = EventLog()
    log.insert(byteArrayOf(5, 6, 7, 8), Min.inc(), Bob)
    log.insert(byteArrayOf(1, 2, 3, 4), Min, Alice)
    assertEquals(2, log.size)
    assertEquals(2, log.acknowledged().size)
    for (i in 0 until 2) {
      val identifier = log.acknowledged()[i]
      val expected =
          when (identifier.site) {
            Alice -> Min
            Bob -> Min.inc()
            else -> fail()
          }
      assertEquals(expected, identifier.seqno)
    }
  }
}
