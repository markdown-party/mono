package markdown.echo.memory.log

import markdown.echo.EchoPreview
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier

/**
 * An implementation of [EventLog] that's empty.
 */
@EchoPreview
internal object EmptyEventLog : EventLog<Nothing> {
    override val sites = emptySet<SiteIdentifier>()
    override fun expected(site: SiteIdentifier) = SequenceNumber.Zero
    override fun get(seqno: SequenceNumber, site: SiteIdentifier) = null
    override fun events(
        seqno: SequenceNumber,
        site: SiteIdentifier,
    ) = emptyList<Pair<EventIdentifier, Nothing>>()
}
