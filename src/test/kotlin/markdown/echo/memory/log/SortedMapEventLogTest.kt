package markdown.echo.memory.log

import kotlin.test.Test
import kotlin.test.assertEquals
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier

class SortedMapEventLogTest {

  @Test
  fun `Inserted event is properly acked`() {
    val log = SortedMapEventLog<Unit>()
    val site = SiteIdentifier(456)
    val seqno = SequenceNumber(123U)

    log[seqno, site] = Unit

    assertEquals(seqno.inc(), log.expected(site))
  }

  @Test
  fun `Maximum seqno is acked`() {
    val log = SortedMapEventLog<Unit>()
    val site = SiteIdentifier(123)
    val low = SequenceNumber(1U)
    val high = SequenceNumber(2U)

    // Insert the highest first.
    log[high, site] = Unit
    log[low, site] = Unit

    assertEquals(high.inc(), log.expected(site))
  }

  @Test
  fun `Expected is zero for un-acked event`() {
    val log = SortedMapEventLog<Unit>()
    val site = SiteIdentifier(456)

    assertEquals(SequenceNumber.Zero, log.expected(site))
  }

  @Test
  fun `Inserted event can be read`() {
    val log = SortedMapEventLog<Int>()
    val site = SiteIdentifier(456)
    val seqno = SequenceNumber(1U)

    log[seqno, site] = 42

    assertEquals(42, log[seqno, site])
  }
}
