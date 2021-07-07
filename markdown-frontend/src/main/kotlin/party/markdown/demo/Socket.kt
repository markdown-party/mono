package party.markdown.demo

import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.ktor.EchoKtorPreview
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.sync
import kotlin.random.Random
import kotlinx.coroutines.launch
import kotlinx.html.js.onClickFunction
import party.markdown.data.Configuration
import party.markdown.data.toExchange
import party.markdown.react.useCoroutineScope
import party.markdown.react.useFlow
import party.markdown.react.useLaunchedEffect
import party.markdown.tree.*
import party.markdown.ui.navigator.navigator
import react.*
import react.dom.button

@OptIn(EchoKtorPreview::class) private val Remote = Configuration.Default.toExchange()

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
      navigator { tree = total }
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
