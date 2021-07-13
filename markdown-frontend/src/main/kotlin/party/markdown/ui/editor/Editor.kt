package party.markdown.ui.editor

import codemirror.basicSetup.basicSetup
import codemirror.lang.markdown.markdown
import codemirror.state.*
import codemirror.state.Transaction.Companion.remote
import codemirror.view.EditorView
import io.github.alexandrepiveteau.echo.core.buffer.toEventIdentifierArray
import io.github.alexandrepiveteau.echo.core.buffer.toMutableGapBuffer
import io.github.alexandrepiveteau.echo.core.causality.isUnspecified
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import party.markdown.data.text.TextApi
import party.markdown.react.useLaunchedEffect
import party.markdown.rga.RGAEvent
import party.markdown.rga.RGANodeRoot
import party.markdown.tree.TreeNode
import party.markdown.tree.TreeNodeIdentifier
import react.*

fun RBuilder.editor(
    block: EditorProps.() -> Unit,
): ReactElement = child(editor) { attrs(block) }

external interface EditorProps : RProps {
  var node: TreeNode?
  var api: TextApi
}

private val Sample =
    StateField.define(
        StateFieldConfig(
            create = { 0 },
            update = { value, tr -> if (tr.docChanged) value + 1 else value },
        ),
    )

private fun handle(view: EditorView, transaction: Transaction) {
  val remote = transaction.annotation(remote)
  if (remote != undefined && remote) {
    // println("Remote operation.")
  }

  // println("Changed ${view.state.field(Sample, required = false)}")
  // println("Identifiers ${view.state.field(RGAStateField).identifiers}")

  // TODO : Look at the changes, and send them remotely.

  if (transaction.docChanged) {
    transaction.changes.iterChanges(
        f = { fA, tA, fB, tB, txt -> /*println("fA: $fA, tA: $tA, fB: $fB, tB: $tB, txt: $txt")*/ },
        individual = true,
    )
  }
  view.update(arrayOf(transaction))
}

private suspend fun publishInsertions(
    document: TreeNodeIdentifier,
    api: TextApi,
    view: EditorView,
): Unit =
    api.edit(document) {
      val state = view.state
      val ids = state.field(RGAStateField).identifiers.toMutableGapBuffer()
      for (i in 0 until ids.size) {
        if (ids[i].isUnspecified) {
          val prev = if (i == 0) RGANodeRoot else ids[i - 1]
          val char = state.doc.sliceString(i, i + 1)[0]
          ids[i] = yield(RGAEvent.Insert(prev, char))
        }
      }
      view.dispatch(
          TransactionSpec {
            annotations = arrayOf(remote.of(true), RGAIdentifiers.of(ids.toEventIdentifierArray()))
          },
      )
    }

private val editor =
    functionalComponent<EditorProps> { props ->
      val view = useRef<EditorView>()

      useLaunchedEffect(listOf(props.node)) {
        while (true) {
          delay(1000) // TODO : Notifications rather than loop.
          val currentView = view.current ?: continue
          val node = props.node ?: continue
          publishInsertions(node.id, props.api, currentView)
        }
      }

      useLaunchedEffect(listOf(props.node)) {
        val node = props.node ?: return@useLaunchedEffect
        props.api.current(node.id).collect { (txt, _) -> println("Text ${txt.concatToString()}") }
      }

      codeMirror {
        this.extensions =
            arrayOf(
                basicSetup,
                markdown(),
                Sample.extension,
                RGAStateField.extension,
            )
        this.dispatch = { handle(view.current!!, it) }
        this.view = view
      }
    }
