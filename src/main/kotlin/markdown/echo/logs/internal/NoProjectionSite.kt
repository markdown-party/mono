package markdown.echo.logs.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import markdown.echo.EchoEventLogPreview
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

  /**
   * Runs an exchange based on a final state machine. The FSM starts with an [initial] state, and
   * performs steps as messages are received or sent.
   *
   * @param initial a lambda that creates the initial FSM state.
   *
   * @param I the type of the input messages.
   * @param O the type of the output messages.
   * @param S the type of the FSM states.
   */
  private fun <I, O, S : State<I, O, T, S>> exchange(initial: suspend () -> S) =
      channelLink<I, O> { inc ->
        var state = initial()
        val insertions = inserted.produceIn(this)

        // Prepare some context information for the step.
        val scope = StepScopeImpl(inc, this, insertions, mutex)
        val log = SentinelMutableEventLog(log, inserted)

        while (true) {
          // Run the effects, until we've moved to an error state or we were explicitly asked to
          // terminate.
          when (val effect = with(state) { scope.step(log) }) {
            is Effect.Move -> state = effect.next
            is Effect.MoveToError -> throw effect.problem
            is Effect.Terminate -> break
          }
        }

        // Clear the jobs registered in channelLink.
        inc.cancel()
        insertions.cancel()
      }

  override fun outgoing() = exchange { OutgoingState() }
  override fun incoming() = exchange { mutex.withLock { IncomingState(log.sites) } }

  @OptIn(EchoEventLogPreview::class)
  override suspend fun event(
      scope: suspend EventScope<T>.(EventLog<T>) -> Unit,
  ) {
    mutex.withLock {
      var next = log.expected
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
