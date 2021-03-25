package markdown.echo.logs.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import markdown.echo.EchoEventLogPreview
import markdown.echo.Message
import markdown.echo.MutableSite
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SiteIdentifier
import markdown.echo.channelLink
import markdown.echo.events.EventScope
import markdown.echo.logs.*
import markdown.echo.projections.OneWayProjection

/**
 * An implementation of [MutableSite] that delegates the management of data to a [MutableEventLog].
 * This way, optimizations can be applied directly at the [EventLog] level.
 *
 * @param identifier the globally unique identifier for this site.
 * @param log the backing [MutableEventLog].
 * @param model the initial [M].
 * @param projection the [OneWayProjection] that's used.
 *
 * @param T the type of the events.
 * @param M the type of the model.
 */
@OptIn(
    EchoEventLogPreview::class,
    ExperimentalCoroutinesApi::class,
    FlowPreview::class,
)
internal class OneWayProjectionSite<T, M>(
    override val identifier: SiteIdentifier,
    private val log: MutableEventLog<T> = mutableEventLogOf(),
    private val model: M,
    private val projection: OneWayProjection<M, Pair<EventIdentifier, T>>,
) : MutableSite<T, M> {

  /** A [Mutex] which ensures serial access to the [log] and the [inserted] value. */
  private val mutex = Mutex()

  /** A [MutableStateFlow] that acts as a sentinel value that new events were inserted. */
  private val inserted = MutableStateFlow<EventIdentifier?>(null)

  override fun outgoing() =
      channelLink<Message.V1.Incoming<T>, Message.V1.Outgoing<T>> { inc ->
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
      channelLink<Message.V1.Outgoing<T>, Message.V1.Incoming<T>> { inc ->
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
      scope: suspend EventScope<T>.(M) -> Unit,
  ) {
    mutex.withLock {
      // TODO : Concurrent modification of log ?
      val model = log.foldl(model, projection::forward)
      var next = log.expected(site = identifier)
      // TODO : fun interface when b/KT-40165 is fixed.
      val impl =
          object : EventScope<T> {
            override suspend fun yield(event: T): EventIdentifier {
              log[next, identifier] = event
              return EventIdentifier(next++, identifier)
            }
          }
      scope(impl, model)
    }
  }
}
