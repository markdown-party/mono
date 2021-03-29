package markdown.echo.logs.internal

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.SelectClause1
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.logs.ImmutableEventLog
import markdown.echo.logs.StepScope

/** An implementation of [StepScope] that delegates behaviors. */
internal class StepScopeImpl<I, O, T>(
    inc: ReceiveChannel<I>,
    out: SendChannel<O>,
    insertions: ReceiveChannel<ImmutableEventLog<T>>,
    private val update: suspend (SequenceNumber, SiteIdentifier, T) -> Unit,
) : StepScope<I, O, T>, ReceiveChannel<I> by inc, SendChannel<O> by out {

  override val onInsert: SelectClause1<ImmutableEventLog<T>> = insertions.onReceive

  override suspend fun set(seqno: SequenceNumber, site: SiteIdentifier, event: T) {
    update(seqno, site, event)
  }
}
