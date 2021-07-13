package party.markdown.ui.editor

import codemirror.basicSetup.basicSetup
import codemirror.lang.markdown.markdown
import codemirror.state.*
import codemirror.state.Transaction.Companion.remote
import codemirror.view.EditorView
import io.github.alexandrepiveteau.echo.core.buffer.toEventIdentifierArray
import io.github.alexandrepiveteau.echo.core.buffer.toMutableGapBuffer
import io.github.alexandrepiveteau.echo.core.causality.isSpecified
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

/**
 * Publish the insertions currently present in the [EditorView]. This will take all the characters
 * with an unspecified event identifier, and sequentially append them to the tree. Doing the
 * insertions sequentially, from left to right, will preserve the interleaving properties of RGA.
 *
 * @param document the [TreeNodeIdentifier] to which the changes are published.
 * @param api the [TextApi] that is used to yield [RGAEvent]s.
 * @param view the [EditorView] that owns the text.
 */
private suspend fun publishLocal(
    document: TreeNodeIdentifier,
    api: TextApi,
    view: EditorView,
): Unit =
    api.edit(document) {
      val state = view.state

      // Integrate all the local insertions.
      val ids = state.field(RGAStateField).identifiers.toMutableGapBuffer()
      for (i in 0 until ids.size) {
        if (ids[i].isUnspecified) {
          val prev = if (i == 0) RGANodeRoot else ids[i - 1]
          val char = state.doc.sliceString(i, i + 1)[0]
          ids[i] = yield(RGAEvent.Insert(prev, char))
        }
      }

      // Integrate all the local removals.
      val removed = state.field(RGAStateField).removed
      for (i in 0 until removed.size) {
        if (removed[i].isSpecified) {
          yield(RGAEvent.Remove(removed[i]))
        }
      }

      // Dispatch the new identifiers synchronously.
      view.dispatch(
          TransactionSpec {
            annotations = arrayOf(remote.of(true), RGAIdentifiers.of(ids.toEventIdentifierArray()))
          },
      )
    }

private suspend fun publishLocalRemovesAndIntegrateRemote(
    document: TreeNodeIdentifier,
    api: TextApi,
    view: EditorView,
) {}

private val editor =
    functionalComponent<EditorProps> { props ->
      val view = useRef<EditorView>()

      useLaunchedEffect(listOf(props.node)) {
        while (true) {
          delay(1000) // TODO : Notifications rather than loop.
          val currentView = view.current ?: continue
          val node = props.node ?: continue
          publishLocal(node.id, props.api, currentView)
        }
      }

      useLaunchedEffect(listOf(props.node)) {
        val node = props.node ?: return@useLaunchedEffect
        props.api.current(node.id).collect { (txt, _) -> println("Text ${txt.concatToString()}") }
      }

      // TODO : Start a new editor when the current node is changed.
      if (props.node != null) {
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
    }
