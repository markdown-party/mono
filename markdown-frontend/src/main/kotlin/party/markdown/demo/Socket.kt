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
import party.markdown.react.useCoroutineScope
import party.markdown.react.useFlow
import party.markdown.react.useLaunchedEffect
import party.markdown.tree.*
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
        initial = MutableTree(),
        projection = TreeProjection,
        transform = MutableTree::toTree,
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

      val total = useFlow(TreeNode.Folder(TreeNodeRoot, emptySet(), null), State.value)
      val title = if (syncing) "Syncing" else "Request sync"

      button {
        attrs { onClickFunction = { requestSync() } }
        +title
      }
      h1 { +"Current tree : $total" }
      button {
        attrs {
          onClickFunction =
              {
                scope.launch {
                  State.event {
                    val id = yield(TreeEvent.NewFile)
                    yield(TreeEvent.Name(id, "Hello file"))
                  }
                }
              }
        }
        +"New file"
      }
      button {
        attrs {
          onClickFunction =
              {
                scope.launch {
                  State.event {
                    val id = yield(TreeEvent.NewFolder)
                    yield(TreeEvent.Name(id, "Hello folder"))
                  }
                }
              }
        }
        +"New folder"
      }
    }

/** Adds a new sockets component. */
fun RBuilder.sockets(): ReactElement = child(socket)
