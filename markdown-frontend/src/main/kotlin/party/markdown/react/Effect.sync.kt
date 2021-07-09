package party.markdown.react

import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.sync
import react.useCallback
import react.useState

/**
 * A hook that will [sync] between multiple [Exchange], and can be restarted on demand using the
 * provided callback.
 *
 * @param local the local [Exchange] that will be synced.
 * @param remote the remove [Exchange] to which we'll be syncing.
 * @param initial whether sync should start right away or not.
 */
fun <I, O> useSync(
    local: Exchange<I, O>,
    remote: Exchange<I, O>,
    initial: Boolean = true,
): Triple<Boolean, () -> Unit, () -> Unit> {
  val (syncing, setSyncing) = useState(initial)
  useLaunchedEffect(listOf(local, remote, syncing)) {
    if (syncing) {
      try {
        sync(remote, local)
      } finally {
        setSyncing(false)
      }
    }
  }
  return Triple(
      syncing,
      useCallback({ setSyncing(true) }, arrayOf(setSyncing)),
      useCallback({ setSyncing(false) }, arrayOf(setSyncing)),
  )
}
