package party.markdown.ui.editor

import codemirror.basicSetup.basicSetup
import codemirror.lang.markdown.markdown
import codemirror.state.*
import codemirror.state.Transaction.Companion.remote
import codemirror.view.EditorView
import io.github.alexandrepiveteau.echo.core.buffer.toEventIdentifierArray
import io.github.alexandrepiveteau.echo.core.buffer.toMutableGapBuffer
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import io.github.alexandrepiveteau.echo.core.causality.isSpecified
import io.github.alexandrepiveteau.echo.core.causality.isUnspecified
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import party.markdown.data.text.TextApi
import party.markdown.react.useLaunchedEffect
import party.markdown.rga.MutableRGA
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

@Suppress("UnnecessaryVariable")
private fun receiveRemote(
    chars: CharArray,
    ids: EventIdentifierArray,
    view: EditorView,
) {
  val changes = mutableListOf<ChangeSpec>()
  val a = ids
  val b = view.state.field(RGAStateField).identifiers.toMutableGapBuffer()

  var aI = 0
  var bI = 0
  while (true) {
    // We're at the end of the loop, since both array have reached their end and the reconciliation
    // process is done.
    if (aI == a.size && bI == b.size) break

    // We still have some characters to process in both array. There are therefore multiple cases to
    // handle :
    //
    // 1. Both characters match :
    //      a. if the [a] character is not removed, keep it.
    //      b. if the [a] character is removed, remove it.
    // 2. Both characters do not match :
    //      a. if the [b] character is pending for insertion, skip it.
    //      b. if the [a] character is removed, skip it.
    //      c. otherwise insert the [a] character.
    if (aI != a.size && bI != b.size) {
      if (a[aI] == b[bI]) {
        if (chars[aI] != MutableRGA.REMOVED) {
          aI++
          bI++
        } else {
          changes.add(ChangeSpec(from = bI, to = bI + 1, insert = null as String?))
          b.remove(offset = bI, size = 1)
        }
      } else {
        when {
          b[bI].isUnspecified -> bI++
          chars[aI] == MutableRGA.REMOVED -> aI++
          else -> {
            changes.add(
                ChangeSpec(
                    from = bI,
                    to = null,
                    insert = charArrayOf(chars[aI]).concatToString(),
                ))
            b.push(a[aI], offset = bI)
            aI++
            bI++
          }
        }
      }
    }

    // We only have some characters to process in the [a] array. We can therefore decide, for each
    // character, to either insert it or skip it.
    //
    // 1. The [a] character is not removed.
    // 2. The [a] character is removed, so skip it.
    else if (aI != a.size && bI == b.size) {
      if (chars[aI] != MutableRGA.REMOVED) {
        changes.add(
            ChangeSpec(
                from = bI,
                to = null,
                insert = charArrayOf(chars[aI]).concatToString(),
            ))
        b.push(a[aI], offset = bI)
        aI++
        bI++
      } else {
        aI++
      }
    }

    // We have some pending characters in the local buffer. We now have two choices : either these
    // characters are not inserted yet (in which case we'll skip them), or they have an identifier.
    // If so, we know that we're working with an [a] that did not contain the latest changes, and we
    // should therefore skip this reconciliation step and wait the next state.
    else if (aI == a.size && bI != b.size) {
      if (b[bI].isUnspecified) {
        bI++
      } else {
        return
      }
    }
  }

  val transactions =
      changes.map {
        TransactionSpec {
          rawChanges = it
          annotations =
              arrayOf(
                  remote.of(true), /* TODO : Identifiers. */
                  RGAIdentifiers.of(b.toEventIdentifierArray()),
              )
          sequential = true
        }
      }

  view.dispatch(*transactions.toTypedArray())
}

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
        props.api.current(node.id).collect { (txt, ids) ->
          val currentView = view.current
          if (currentView != null) receiveRemote(txt, ids, currentView)
        }
      }

      if (props.node != null) {
        codeMirror {
          this.id = props.node
          this.extensions =
              arrayOf(
                  basicSetup,
                  markdown(),
                  RGAStateField.extension,
              )
          this.view = view
        }
      }
    }
