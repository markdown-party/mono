package party.markdown.demo

import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.ktor.wssExchange
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.serialization.decodeFromFrame
import io.github.alexandrepiveteau.echo.sync
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import kotlin.random.Random
import kotlinx.coroutines.launch
import kotlinx.html.js.onClickFunction
import party.markdown.MarkdownEvent.Decrement
import party.markdown.MarkdownEvent.Increment
import party.markdown.MarkdownProjection
import party.markdown.react.useCoroutineScope
import party.markdown.react.useFlow
import party.markdown.react.useLaunchedEffect
import react.*
import react.dom.button
import react.dom.h1

private val Client = HttpClient(Js) { install(WebSockets) }
private val Remote =
    Client.wssExchange(
            receiver = {
              port = 443
              url {
                host = "api.markdown.party"
                path("receiver")
              }
            },
            sender = {
              port = 443
              url {
                host = "api.markdown.party"
                path("sender")
              }
            },
        )
        .decodeFromFrame()

private val State =
    mutableSite(
        identifier = Random.nextSiteIdentifier(),
        initial = 0,
        projection = MarkdownProjection,
    )

private fun useSync(
    initial: Boolean = true,
): Pair<Boolean, () -> Unit> {
  val (syncing, setSyncing) = useState(initial)
  useLaunchedEffect(listOf(syncing)) {
    if (syncing) {
      try {
        sync(Remote, State)
      } finally {
        setSyncing(false)
      }
    }
  }
  return syncing to { setSyncing(true) }
}

/** A [functionalComponent] that displays a very simple websockets demonstration. */
private val socket =
    functionalComponent<RProps> {
      val scope = useCoroutineScope()
      val (syncing, requestSync) = useSync()

      val total = useFlow(0, State.value)
      val title = if (syncing) "Syncing" else "Request sync"

      button {
        attrs { onClickFunction = { requestSync() } }
        +title
      }
      h1 { +"Current total is $total" }
      button {
        attrs { onClickFunction = { scope.launch { State.event { yield(Decrement) } } } }
        +"Decrement"
      }
      button {
        attrs { onClickFunction = { scope.launch { State.event { yield(Increment) } } } }
        +"Increment"
      }
    }

/** Adds a new sockets component. */
fun RBuilder.sockets(): ReactElement = child(socket)
