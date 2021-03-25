package markdown.echo.logs.internal

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.sync.Mutex
import markdown.echo.causal.EventIdentifier
import markdown.echo.logs.StepScope

/** An implementation of [StepScope] that delegates behaviors. */
internal class StepScopeImpl<I, O>(
    inc: ReceiveChannel<I>,
    out: SendChannel<O>,
    insertions: ReceiveChannel<EventIdentifier?>,
    mutex: Mutex,
) : StepScope<I, O>, ReceiveChannel<I> by inc, SendChannel<O> by out, Mutex by mutex {
  override val onInsert: SelectClause1<EventIdentifier?> = insertions.onReceive
}
