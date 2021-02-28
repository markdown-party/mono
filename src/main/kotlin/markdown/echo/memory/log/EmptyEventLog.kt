package markdown.echo.memory.log

import markdown.echo.EchoPreview
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier

/**
 * An implementation of [EventLog] that's empty.
 */
@EchoPreview
internal object EmptyEventLog : EventLog<Nothing> {
    override fun expected(site: SiteIdentifier) = SequenceNumber.Zero
    override fun get(seqno: SequenceNumber, site: SiteIdentifier) = null
}
