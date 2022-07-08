package party.markdown.ui.topBar

import androidx.compose.runtime.*
import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.MutableSite
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.compose.web.dom.*
import party.markdown.MarkdownParty
import party.markdown.MarkdownPartyEvent
import party.markdown.data.Configuration
import party.markdown.rememberSyncState

private val TickDuration = 500.milliseconds
private val SyncIndicatorDuration = 500.milliseconds
private val RetryDuration = 5.seconds

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
    override fun receive(
        incoming: Flow<O>,
    ) = this@proxy.receive(incoming.onEach { update() }).onEach { update() }
    override fun send(
        incoming: Flow<I>,
    ) = this@proxy.send(incoming.onEach { update() }).onEach { update() }
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
private fun every(duration: Duration): Flow<Instant> = flow {
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
@Composable
private fun collectSyncIcon(
    syncing: Boolean,
    now: Flow<Instant>,
    latest: StateFlow<Instant>,
): SyncIcon {
  val nowValue by now.collectAsState(Clock.System.now())
  val latestValue by latest.collectAsState()
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
@Composable
private fun RetrySync(debug: Boolean, syncing: Boolean, request: () -> Unit) {
  LaunchedEffect(debug, syncing, request) {
    if (!debug && !syncing) {
      delay(RetryDuration)
      request()
    }
  }
}

/** Displays a button with the given [text], which runs a certain action. */
@Composable
private fun DebugButton(
    text: String,
    onClick: () -> Unit,
) {
  Button(
      attrs = {
        classes(
            "transition-all",
            "text-white",
            "px-4",
            "py-2",
            "bg-blue-500",
            "hover:bg-blue-600",
            "rounded",
            "shadow",
            "hover:shadow-lg",
        )
        onClick { onClick() }
      }) { Text(text) }
}

// Component.

/**
 * A composable that display the current sync status. More specifically, it uses an icon to display
 * whether the server is currently connected with the local client, and indicates when the last sync
 * occurred as well as the retry policy.
 *
 * Please note that this component will own the sync state. Therefore, you should probably keep the
 * component present on the screen, or hoist the sync state somewhere else if required.
 */
@Composable
fun SyncIndicator(
    local: MutableSite<MarkdownPartyEvent, MarkdownParty>,
    configuration: Configuration,
    debugMode: Boolean,
) {
  // TODO : Support sync indicators.
  val latest = remember { MutableStateFlow(Clock.System.now()) }
  val state = rememberSyncState(local, configuration)
  val now = remember { every(TickDuration) }
  val icon = collectSyncIcon(state.syncing, now, latest)

  // Ensure we run sync whenever possible.
  RetrySync(
      debug = debugMode,
      syncing = state.syncing,
      request = { state.start() },
  )

  Div(
      attrs = {
        classes(
            "flex",
            "flex-row",
            "items-center",
            "space-x-4",
            "justify-end",
            "select-none",
            "p-4",
        )
      }) {
    Span(
        attrs = {
          classes(
              "font-semibold",
              "uppercase",
              "w-24",
              "text-right",
          )
        }) { Text(icon.label) }
    Img(src = icon.url, alt = "Sync status")
    if (debugMode) {
      DebugButton(text = "Start now", onClick = { state.start() })
      DebugButton(text = "Stop sync", onClick = { state.stop() })
    }
  }
}
