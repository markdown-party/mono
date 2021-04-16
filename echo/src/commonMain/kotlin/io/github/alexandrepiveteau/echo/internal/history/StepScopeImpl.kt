package io.github.alexandrepiveteau.echo.internal.history

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.ImmutableEventLog
import io.github.alexandrepiveteau.echo.protocol.fsm.StepScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.SelectClause1

/** An implementation of [StepScope] that delegates behaviors. */
internal class StepScopeImpl<I, O, T, C>(
    inc: ReceiveChannel<I>,
    out: SendChannel<O>,
    insertions: ReceiveChannel<ImmutableEventLog<T, C>>,
    private val update: suspend (SequenceNumber, SiteIdentifier, T) -> Unit,
) : StepScope<I, O, T, C>, ReceiveChannel<I> by inc, SendChannel<O> by out {

  override val onInsert: SelectClause1<ImmutableEventLog<T, C>> = insertions.onReceive

  override suspend fun set(seqno: SequenceNumber, site: SiteIdentifier, event: T) {
    update(seqno, site, event)
  }
}
