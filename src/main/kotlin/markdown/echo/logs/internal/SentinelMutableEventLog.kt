package markdown.echo.logs.internal

import kotlinx.coroutines.flow.MutableStateFlow
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.logs.EventLog
import markdown.echo.logs.MutableEventLog

/** A [MutableEventLog] that updates a [sentinel] whenever a missing value is set. */
internal class SentinelMutableEventLog<T>(
    private val backing: MutableEventLog<T>,
    private val sentinel: MutableStateFlow<EventIdentifier?>,
) : MutableEventLog<T>, EventLog<T> by backing {
  override fun set(seqno: SequenceNumber, site: SiteIdentifier, body: T) {
    // This considers that a Mutex is hold when performing the set operation.
    if (backing[seqno, site] != null) {
      sentinel.value = EventIdentifier(seqno, site)
    }
    backing[seqno, site] = body
  }
}
