package party.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.github.alexandrepiveteau.echo.Exchange
import io.github.alexandrepiveteau.echo.indexeddb.load
import io.github.alexandrepiveteau.echo.indexeddb.save
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * An effect which loads the events stored in the browser in the provided [Exchange].
 *
 * @param session the identifier of the current session.
 * @param exchange the [Exchange] in which the events should be loaded.
 */
@Composable
fun LoadEventsEffect(session: String, exchange: Exchange<Incoming, Outgoing>) {
  LaunchedEffect(session, exchange) { runCatching { load(session, exchange) } }
}

/** The delay between each save. */
private val SaveDelay = 10.seconds

/**
 * An effect which stores the events from the provided [Exchange] to the browser.
 *
 * @param session the identifier of the current session.
 * @param exchange the [Exchange] from which the events are taken.
 */
@Composable
fun SaveEventsEffect(session: String, exchange: Exchange<Incoming, Outgoing>) {
  LaunchedEffect(session, exchange) {
    while (true) {
      delay(SaveDelay)
      runCatching { save(session, exchange) }
    }
  }
}
