package markdown.echo.logs.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import markdown.echo.Message.V1.Incoming as Inc
import markdown.echo.Message.V1.Outgoing as Out
import markdown.echo.MutableEventLogSite
import markdown.echo.MutableSite
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SiteIdentifier
import markdown.echo.channelLink
import markdown.echo.events.EventScope
import markdown.echo.logs.*

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
internal class NoProjectionSite<T>(
    override val identifier: SiteIdentifier,
    private val log: MutableEventLog<T> = mutableEventLogOf()
) : MutableEventLogSite<T> {

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
      // TODO : Concurrent modification of log ?
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
