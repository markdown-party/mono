package markdown.echo.logs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import markdown.echo.Message.V1.Incoming as Inc
import markdown.echo.Message.V1.Outgoing as Out
import markdown.echo.MutableSite
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SequenceNumber
import markdown.echo.causal.SiteIdentifier
import markdown.echo.channelLink
import markdown.echo.events.EventScope

/** An implementation of [StepScope] that delegates behaviors. */
private class StepScopeImpl<I, O>(
    inc: ReceiveChannel<I>,
    out: SendChannel<O>,
    insertions: ReceiveChannel<EventIdentifier?>,
    mutex: Mutex,
) : StepScope<I, O>, ReceiveChannel<I> by inc, SendChannel<O> by out, Mutex by mutex {
  override val onInsert: SelectClause1<EventIdentifier?> = insertions.onReceive
}

/** A [MutableEventLog] that updates a [sentinel] whenever a missing value is set. */
private class SentinelMutableEventLog<T>(
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

/**
 * An implementation of [MutableSite] that delegates the management of data to a [MutableEventLog].
 * This way, optimizations can be applied directly at the [EventLog] level.
 *
 * @param identifier the globally unique identifier for this site.
 * @param log the backing [MutableEventLog].
 *
 * @param T the type of the events.
 */
@OptIn(
    ExperimentalCoroutinesApi::class,
    FlowPreview::class,
)
internal class MutableEventLogSiteImpl<T>(
    override val identifier: SiteIdentifier,
    private val log: MutableEventLog<T> = mutableEventLogOf()
) : MutableSite<T, EventLog<T>> {

  /** A [Mutex] which ensures serial access to the [log] and the [inserted] value. */
  private val mutex = Mutex()

  /** A [MutableStateFlow] that acts as a sentinel value that new events were inserted. */
  private val inserted = MutableStateFlow<EventIdentifier?>(null)

  override fun outgoing() =
      channelLink<Inc<T>, Out<T>> { inc ->
        // Iterate within the FSM until we're over.
        var state: OutgoingState<T> = OutgoingState()
        val insertions = inserted.produceIn(this)
        while (state.keepGoing(inc, this)) {
          val scope = StepScopeImpl(inc, this, insertions, mutex)
          val log = SentinelMutableEventLog(log, inserted)
          state = state.step().invoke(scope, log)
        }
        inc.cancel()
        insertions.cancel()
      }

  override fun incoming() =
      channelLink<Out<T>, Inc<T>> { inc ->
        // Iterate within the FSM until we're over.
        var state: IncomingState<T> = mutex.withLock { IncomingState(log.sites) }
        val insertions = inserted.produceIn(this)
        while (state.keepGoing(inc, this)) {
          val scope = StepScopeImpl(inc, this, insertions, mutex)
          val log = SentinelMutableEventLog(log, inserted)
          state = state.step().invoke(scope, log)
        }
        inc.cancel()
        insertions.cancel()
      }

  override suspend fun event(
      scope: suspend EventScope<T>.(EventLog<T>) -> Unit,
  ) {
    mutex.withLock {
      var next = log.expected(site = identifier)
      // TODO : fun interface when b/KT-40165 is fixed.
      val impl =
          object : EventScope<T> {
            override suspend fun yield(event: T): EventIdentifier {
              log[next, identifier] = event
              return EventIdentifier(next++, identifier)
            }
          }
      scope(impl, log)
    }
  }
}
