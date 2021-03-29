package markdown.echo.logs

import markdown.echo.EchoEventLogPreview
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier

/** An implementation of [ImmutableEventLog] that's empty. */
internal object EmptyEventLog : ImmutableEventLog<Nothing> {

  override val sites = emptySet<SiteIdentifier>()

  @EchoEventLogPreview override val expected = SequenceNumber.Zero

  override fun expected(site: SiteIdentifier) = SequenceNumber.Zero

  override fun get(seqno: SequenceNumber, site: SiteIdentifier) = null

  override fun events(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ) = emptyList<Pair<EventIdentifier, Nothing>>()

  override fun toPersistentEventLog(): PersistentEventLog<Nothing> = persistentEventLogOf()

  @EchoEventLogPreview
  override fun <R> foldl(
      base: R,
      step: (Pair<EventIdentifier, Nothing>, R) -> R,
  ): R = base
}
