package party.markdown.ui.topBar

import androidx.compose.runtime.*
import io.github.alexandrepiveteau.echo.MutableSite
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.web.dom.*
import party.markdown.MarkdownParty
import party.markdown.MarkdownPartyEvent
import party.markdown.data.Configuration
import party.markdown.rememberSyncState

private val RetryDuration = 5.seconds

/**
 * An enumeration representing the different sync icons that might get displayed as an indicator of
 * the current sync status.
 */
data class SyncIcon(
    val url: String,
    val label: String,
) {

  companion object {

    /** We're currently connected to the backend, but there's no activity. */
    val Online =
        SyncIcon(
            url = "/icons/sync-status-online.svg",
            label = "Online",
        )

    /** There are [count] participants currently connected. */
    fun participants(count: Int) =
        SyncIcon(
            url = "/icons/sync-status-people.svg",
            label = "${count + 1}\u00a0Online",
        )

    /** There is no active connection to the backend. */
    val Offline =
        SyncIcon(
            "/icons/sync-status-offline.svg",
            "Offline",
        )
  }
}

/**
 * Returns the [SyncIcon] corresponding to the current
 *
 * @param syncing true if the client is currently syncing.
 * @param participants the number of participants in the editing session.
 *
 * @return the [SyncIcon] corresponding to the current state.
 */
private fun syncIcon(syncing: Boolean, participants: Int): SyncIcon {
  return if (!syncing) SyncIcon.Offline
  else if (participants > 0) SyncIcon.participants(participants) else SyncIcon.Online
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
  val state = rememberSyncState(local, configuration)
  val icon = syncIcon(state.syncing, state.participantsCount)

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
        },
    ) {
      Text(icon.label)
    }
    Img(src = icon.url, alt = "Sync status")
    if (debugMode) {
      DebugButton(text = "Start now", onClick = { state.start() })
      DebugButton(text = "Stop sync", onClick = { state.stop() })
    }
  }
}
