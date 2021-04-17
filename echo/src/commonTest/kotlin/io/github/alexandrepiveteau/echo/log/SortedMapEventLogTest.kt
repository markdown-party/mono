package io.github.alexandrepiveteau.echo.log

import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.Change
import io.github.alexandrepiveteau.echo.logs.Change.Companion.skipped
import io.github.alexandrepiveteau.echo.logs.persistentEventLogOf
import kotlin.test.Test
import kotlin.test.assertEquals

class SortedMapEventLogTest {

  @Test
  fun nonEmptyLog_isNotEmpty() {
    val log = persistentEventLogOf(EventIdentifier(SequenceNumber(0u), SiteIdentifier(1)) to 123)
    assertEquals(123, log[SiteIdentifier(1), SequenceNumber(0u)]?.body)
  }

  @Test
  fun insertedEvent_isAcked() {
    var log = persistentEventLogOf<Unit>()
    val site = SiteIdentifier(456)
    val seqno = SequenceNumber(123U)

    log = log.set(site, seqno, Unit, skipped())

    assertEquals(seqno.inc(), log.expected(site))
  }

  @Test
  fun maximumSeqno_isAcked() {
    var log = persistentEventLogOf<Unit>()
    val site = SiteIdentifier(123)
    val low = SequenceNumber(1U)
    val high = SequenceNumber(2U)

    // Insert the highest first.
    log = log.set(site, high, Unit, skipped())
    log = log.set(site, low, Unit, skipped())

    assertEquals(high.inc(), log.expected(site))
  }

  @Test
  fun nonAckedEvent_hasZeroSeqno() {
    val log = persistentEventLogOf<Unit>()
    val site = SiteIdentifier(456)

    assertEquals(SequenceNumber.Zero, log.expected(site))
  }

  @Test
  fun insertedEvent_isPresent() {
    var log = persistentEventLogOf<Int>()
    val site = SiteIdentifier(456)
    val seqno = SequenceNumber(1U)

    log = log.set(site, seqno, 42, skipped())

    assertEquals(42, log[site, seqno]?.body)
  }
}
