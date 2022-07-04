package party.markdown

import androidx.compose.runtime.*
import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.sync

/**
 * An interface representing the synchronisation state. Instances of [SyncState] let consumers know
 * whether some [Exchange] are currently syncing, as well as request the sync process to [start] or
 * [stop].
 */
@Stable
interface SyncState {

  /** True if the two [Exchange]s are currently connected. */
  val syncing: Boolean

  /** Requests the two [Exchange]s to start a sync process. */
  fun start()

  /** Request the two [Exchange]s to stop a currently started sync process. */
  fun stop()
}

private class SnapshotSyncState(initial: Boolean) : SyncState {

  override var syncing by mutableStateOf(initial)
    private set

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
 * @param remote the remove [Exchange] to which we'll be syncing.
 * @param initial whether sync should start right away or not.
 */
@Composable
fun <I, O> rememberSyncState(
    local: Exchange<I, O>,
    remote: Exchange<I, O>,
    initial: Boolean = true,
): SyncState {
  val state = remember(local, remote) { SnapshotSyncState(initial) }
  LaunchedEffect(local, remote, state.syncing) {
    if (state.syncing) {
      try {
        sync(remote, local)
      } catch (problem: Throwable) {
        // Ignored.
      } finally {
        state.stop()
      }
    }
  }
  return state
}
