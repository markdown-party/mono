package party.markdown

import androidx.compose.runtime.*
import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import io.github.alexandrepiveteau.echo.sync
import party.markdown.data.Configuration
import party.markdown.data.sync

/**
 * An interface representing the synchronisation state. Instances of [SyncState] let consumers know
 * whether some [Exchange] are currently syncing, as well as request the sync process to [start] or
 * [stop].
 */
@Stable
interface SyncState {

  /** True if the two [Exchange]s are currently connected. */
  val syncing: Boolean

  /** The number of participants in the session. */
  @Deprecated("Not provided by the server.") val participantsCount: Int

  /** Requests the two [Exchange]s to start a sync process. */
  fun start()

  /** Request the two [Exchange]s to stop a currently started sync process. */
  fun stop()
}

private class SnapshotSyncState(initial: Boolean) : SyncState {

  override var syncing by mutableStateOf(initial)
    private set

  @Deprecated("Not provided by the server.") override val participantsCount = 0

  override fun start() {
    syncing = true
  }

  override fun stop() {
    syncing = false
  }
}

/**
 * Returns a [SyncState] that will [sync] between multiple [Exchange]s, and can be restarted on
 * demand using the provided callbacks.
 *
 * @param local the local [Exchange] that will be synced.
 * @param configuration the remote [Configuration] which is used to sync data.
 * @param initial whether sync should start right away or not.
 */
@Composable
fun rememberSyncState(
    local: Exchange<Incoming, Outgoing>,
    configuration: Configuration,
    initial: Boolean = true,
): SyncState {
  val state = remember(local, configuration) { SnapshotSyncState(initial) }
  LaunchedEffect(local, configuration, state.syncing) {
    if (state.syncing) {
      try {
        configuration.sync(local)
      } catch (problem: Throwable) {
        // Ignored.
        println("Stopped with $problem")
      } finally {
        state.stop()
      }
    }
  }
  return state
}
