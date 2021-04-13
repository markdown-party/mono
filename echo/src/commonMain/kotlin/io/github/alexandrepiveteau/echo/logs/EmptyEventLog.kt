package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.EchoEventLogPreview
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.EventLog.Entry

/** An implementation of [ImmutableEventLog] that's empty. */
internal object EmptyEventLog : ImmutableEventLog<Nothing>, AbstractEventLog<Nothing>() {

  override val sites = emptySet<SiteIdentifier>()

  @EchoEventLogPreview override val expected = SequenceNumber.Zero

  override fun expected(site: SiteIdentifier) = SequenceNumber.Zero

  override fun get(seqno: SequenceNumber, site: SiteIdentifier) = null

  override fun events(
      seqno: SequenceNumber,
      site: SiteIdentifier,
  ) = emptyList<Entry<Nothing>>()

  override fun toPersistentEventLog(): PersistentEventLog<Nothing> = persistentEventLogOf()
}
