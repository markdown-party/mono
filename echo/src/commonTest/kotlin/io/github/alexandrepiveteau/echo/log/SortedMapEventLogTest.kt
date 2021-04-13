package io.github.alexandrepiveteau.echo.log

import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.PersistentEventLog
import io.github.alexandrepiveteau.echo.logs.PersistentMapEventLog
import io.github.alexandrepiveteau.echo.logs.persistentEventLogOf
import kotlin.test.Test
import kotlin.test.assertEquals

class SortedMapEventLogTest {

  @Test
  fun `Non empty log is actually non empty`() {
    val log = persistentEventLogOf(EventIdentifier(SequenceNumber(0u), SiteIdentifier(1)) to 123)
    assertEquals(123, log[SequenceNumber(0u), SiteIdentifier(1)]?.body)
  }

  @Test
  fun `Inserted event is properly acked`() {
    var log = persistentEventLogOf<Unit>()
    val site = SiteIdentifier(456)
    val seqno = SequenceNumber(123U)

    log = log.set(seqno, site, Unit)

    assertEquals(seqno.inc(), log.expected(site))
  }

  @Test
  fun `Maximum seqno is acked`() {
    var log = persistentEventLogOf<Unit>()
    val site = SiteIdentifier(123)
    val low = SequenceNumber(1U)
    val high = SequenceNumber(2U)

    // Insert the highest first.
    log = log.set(high, site, Unit)
    log = log.set(low, site, Unit)

    assertEquals(high.inc(), log.expected(site))
  }

  @Test
  fun `Expected is zero for un-acked event`() {
    val log: PersistentEventLog<Unit> = PersistentMapEventLog()
    val site = SiteIdentifier(456)

    assertEquals(SequenceNumber.Zero, log.expected(site))
  }

  @Test
  fun `Inserted event can be read`() {
    var log = persistentEventLogOf<Int>()
    val site = SiteIdentifier(456)
    val seqno = SequenceNumber(1U)

    log = log.set(seqno, site, 42)

    assertEquals(42, log[seqno, site]?.body)
  }
}
