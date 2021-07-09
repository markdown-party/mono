@file:OptIn(ExperimentalTime::class)

package party.markdown.ui.topBar

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.onEach
import io.github.alexandrepiveteau.echo.protocol.Message
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.html.js.onClickFunction
import party.markdown.MarkdownParty
import party.markdown.MarkdownPartyEvent
import party.markdown.react.useFlow
import party.markdown.react.useLaunchedEffect
import party.markdown.react.useSync
import react.*
import react.dom.button
import react.dom.div
import react.dom.img
import react.dom.span

private val TickDuration = Duration.milliseconds(500)
private val SyncIndicatorDuration = Duration.milliseconds(500)
private val RetryDuration = Duration.seconds(5)

/**
 * A [ReactElement] that display the current sync status. More specifically, it uses an icon to
 * display whether the server is currently connected with the local client, and indicates when the
 * last sync occurred as well as the retry policy.
 *
 * Please note that this component will own the sync state. Therefore, you should probably keep the
 * component present on the screen, or hoist the sync state somewhere else if required.
 *
 * @param block the configuration block for the indicator.
 */
fun RBuilder.syncIndicator(
    block: SyncIndicatorProps.() -> Unit,
): ReactElement = child(component) { attrs(block) }

/** The properties available for the [RBuilder.syncIndicator]. */
external interface SyncIndicatorProps : RProps {
  var local: MutableSite<MarkdownPartyEvent, MarkdownParty>
  var remote: Exchange<Message.Incoming, Message.Outgoing>
  var debugMode: Boolean
}

// Component.

/**
 * Proxies the [Exchange] to observe the [Instant] of the latest message to was sent or received
 * through the [Exchange]. The [Instant] can then be observed through a [StateFlow], that will be
 * updated with a new [Instant] on each message.
 *
 * Additionally, the returned [StateFlow] is guaranteed to return monotonically increasing [Instant]
 * values.
 *
 * @return A [Pair] of the proxied [Exchange] and the last message [Instant].
 */
private fun <I, O> Exchange<I, O>.proxy(): Pair<Exchange<I, O>, StateFlow<Instant>> {
  val lastMessage = MutableStateFlow(Clock.System.now())
  val mutex = Mutex()
  val update = suspend {
    val current = Clock.System.now()
    mutex.withLock {
      val value = lastMessage.value
      if (current > value) lastMessage.value = current
    }
  }
  return object : Exchange<I, O> {
    override fun outgoing() = this@proxy.outgoing().onEach({ update() }, { update() })
    override fun incoming() = this@proxy.incoming().onEach({ update() }, { update() })
  } to lastMessage
}

/**
 * Returns a [Flow] that regularly ticks with the provided [duration] interval. However, the
 * returned [Flow] does not try to keep in sync with the system clock, but rather only waits for the
 * given interval [duration] after each emission.
 *
 * @param duration how long the [Flow] waits between emissions.
 *
 * @return A cold [Flow] with the current [Instant].
 */
@ExperimentalTime
private fun every(
    duration: Duration,
): Flow<Instant> = flow {
  while (true) {
    emit(Clock.System.now())
    delay(duration)
  }
}

/**
 * An enumeration representing the different sync icons that might get displayed as an indicator of
 * the current sync status.
 */
enum class SyncIcon(
    val url: String,
    val label: String,
) {

  /** We're currently connected to the backend, but there's no activity. */
  Online(
      "/icons/sync-status-online.svg",
      "Online",
  ),

  // We could add finer granularity with the addition of TrafficUp and TrafficDown icons.

  /** Some messages are being sent and received. */
  TrafficBoth(
      "/icons/sync-status-traffic-both.svg",
      "Syncing",
  ),

  /** There is no active connection to the backend. */
  Offline(
      "/icons/sync-status-offline.svg",
      "Offline",
  ),
}

/**
 * Combines the current [syncing] state, and two [Flow] returning the current timestamp and the
 * timestamp of the last message that was exchanged.
 *
 * @return the [SyncIcon] corresponding to the current state.
 */
fun combineToSyncIcon(
    syncing: Boolean,
    now: Flow<Instant>,
    latest: StateFlow<Instant>,
): SyncIcon {
  val nowValue = useFlow(initial = Clock.System.now(), now)
  val latestValue = useFlow(latest)
  if (!syncing) return SyncIcon.Offline
  if (nowValue - SyncIndicatorDuration < latestValue) return SyncIcon.TrafficBoth
  return SyncIcon.Online
}

/**
 * An effect that will launch a new sync operation on sync failures.
 *
 * @param debug if `true`, the retry sync effect will not be run.
 * @param syncing whether we are currently syncing or not.
 * @param request the callback used to request sync.
 */
fun useRetrySync(debug: Boolean, syncing: Boolean, request: () -> Unit) {
  useLaunchedEffect(listOf(debug, syncing, request)) {
    if (!debug && !syncing) {
      delay(RetryDuration)
      request()
    }
  }
}

/** Displays a button with the given [text], which runs a certain action. */
private fun RBuilder.debugButton(
    text: String,
    action: () -> Unit,
): ReactElement {
  return button(
      classes =
          "transition-all text-white px-4 py-2 bg-blue-500 hover:bg-blue-600 rounded shadow hover:shadow-lg") {
    attrs { onClickFunction = { action() } }
    +text
  }
}

private val component =
    functionalComponent<SyncIndicatorProps> { props ->
      val (proxy, latest) = useMemo(props.remote) { props.remote.proxy() }
      val (syncing, requestSync, requestStopSync) = useSync(props.local, proxy)
      val now = useMemo { every(TickDuration) }
      val icon = combineToSyncIcon(syncing, now, latest)

      // Ensure we run sync whenever possible.
      useRetrySync(debug = props.debugMode, syncing = syncing, request = requestSync)

      div(classes = "flex flex-row items-center space-x-4 justify-end select-none p-4") {
        span(classes = "font-semibold uppercase") { +icon.label }
        img(alt = "Sync status", src = icon.url) {}
        if (props.debugMode) {
          debugButton("Sync now", requestSync)
          debugButton("Stop sync", requestStopSync)
        }
      }
    }
