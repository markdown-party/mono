package party.markdown.ui.editor

import LocalSiteIdentifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import codemirror.basicSetup.basicSetup
import codemirror.lang.markdown.markdown
import codemirror.state.ChangeSpec
import codemirror.state.StateField
import codemirror.state.Transaction.Companion.remote
import codemirror.state.TransactionSpec
import codemirror.state.annotations
import codemirror.theme.oneDark.oneDark
import codemirror.view.EditorView
import io.github.alexandrepiveteau.echo.core.buffer.toEventIdentifierArray
import io.github.alexandrepiveteau.echo.core.buffer.toMutableGapBuffer
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import io.github.alexandrepiveteau.echo.core.causality.isSpecified
import io.github.alexandrepiveteau.echo.core.causality.isUnspecified
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock
import party.markdown.cursors.CursorEvent
import party.markdown.cursors.CursorRoot
import party.markdown.cursors.Cursors
import party.markdown.data.text.TextApi
import party.markdown.rga.MutableRGA
import party.markdown.rga.RGAEvent
import party.markdown.rga.RGANodeRoot
import party.markdown.tree.TreeNode
import party.markdown.tree.TreeNodeIdentifier

/**
 * Publish the insertions currently present in the [EditorView]. This will take all the characters
 * with an unspecified event identifier, and sequentially append them to the tree. Doing the
 * insertions sequentially, from left to right, will preserve the interleaving properties of RGA.
 *
 * @param cursors the [StateField] which provides access to the cursors.
 * @param document the [TreeNodeIdentifier] to which the changes are published.
 * @param api the [TextApi] that is used to yield [RGAEvent]s.
 * @param view the [EditorView] that owns the text.
 */
private suspend fun publishLocal(
    cursors: StateField<CursorsState>,
    document: TreeNodeIdentifier,
    api: TextApi,
    view: EditorView,
): Unit =
    api.edit(document) {
      val state = view.state
      val cursorsState = view.state.field(cursors)

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

      var cursorsSet = cursorsState.cursors
      val actorCursor = cursorsSet.firstOrNull { it.actor == cursorsState.actor }
      val actorPos = actorCursor?.anchor?.let { ids.toEventIdentifierArray().indexOfCursor(it) }
      val expected = view.state.selection.main.head

      if (actorPos != expected) {
        val anchor = if (expected == 0) CursorRoot else ids[expected - 1]
        yield(CursorEvent.MoveAfter(document, anchor))
        cursorsSet =
            cursorsSet
                .asSequence()
                .filter { it.actor != cursorsState.actor }
                .plusElement(Cursors.Cursor(cursorsState.actor, Clock.System.now(), anchor))
                .toSet()
      }

      // Dispatch the new identifiers synchronously.
      view.dispatch(
          TransactionSpec {
            annotations =
                arrayOf(
                    remote.of(true),
                    RGAIdentifiers.of(
                        RGAIdentifiersData(
                            ids = ids.toEventIdentifierArray(),
                            removed = EventIdentifierArray(0),
                        )),
                    CursorAnnotation.of(cursorsSet),
                )
          },
      )
    }

@Suppress("UnnecessaryVariable")
private fun receiveRemote(
    chars: CharArray,
    ids: EventIdentifierArray,
    cursors: Set<Cursors.Cursor>,
    view: EditorView,
) {
  val changes = mutableListOf<ChangeSpec>()
  val field = view.state.field(RGAStateField)

  val a = ids
  val b = field.identifiers.toMutableGapBuffer()

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
    //      c. if the [a] character is pending for removal, skip it.
    //      d. otherwise insert the [a] character.
    if (aI != a.size && bI != b.size) {
      if (a[aI] == b[bI]) {
        if (chars[aI] != MutableRGA.REMOVED && a[aI] !in field.removed) {
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
          a[aI] in field.removed -> aI++
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
      if (chars[aI] != MutableRGA.REMOVED && a[aI] !in field.removed) {
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

  // We'll always still want to update the cursors nevertheless. Indeed, if no changes were
  // recorded, no transaction would contain the updated cursors with the move operations.
  val updateCursor = TransactionSpec {
    annotations = arrayOf(CursorAnnotation.of(cursors))
    sequential = true
  }

  val transactions =
      changes
          .map {
            TransactionSpec {
              rawChanges = it
              annotations =
                  arrayOf(
                      remote.of(true), /* TODO : Identifiers. */
                      RGAIdentifiers.of(
                          RGAIdentifiersData(
                              ids = b.toEventIdentifierArray(),
                              removed = field.removed,
                          )),
                      CursorAnnotation.of(cursors),
                  )
              sequential = true
            }
          }
          .plus(updateCursor)

  view.dispatch(*transactions.toTypedArray())
}

@Composable
fun Editor(
    treeNode: TreeNode?,
    api: TextApi,
) {
  if (treeNode != null) {
    val site = LocalSiteIdentifier.current
    val field = remember { cursorsStateField(site) }
    val view =
        rememberEditorView(
                treeNode,
                extensions =
                    arrayOf(
                        basicSetup,
                        EditorView.lineWrapping,
                        // oneDark,
                        markdown(),
                        RGAStateField.extension,
                        cursorTooltipBaseTheme,
                        field.extension,
                        NowStateField.extension,
                    ),
                attrs = { classes("flex-grow", "h-full", "min-w-0", "max-h-full") },
            )
            .value

    LaunchedEffect(treeNode, view) {
      while (true) {
        delay(1000) // TODO : Notifications rather than loop.
        val currentView = view ?: continue
        publishLocal(field, treeNode.id, api, currentView)
      }
    }

    LaunchedEffect(treeNode, view) {
      api.current(treeNode.id).collect { (txt, ids, crs) ->
        if (view != null) {
          receiveRemote(txt, ids, crs, view)
        }
      }
    }

    LaunchedEffect(treeNode, view) {

      // Update the clock from the editor, at a regular pace, and in an atomic fashion.
      while (true) {
        @OptIn(ExperimentalTime::class) delay(DelayTick)
        view?.dispatch(
            TransactionSpec { annotations = arrayOf(SetNowAnnotation.of(Clock.System.now())) },
        )
      }
    }
  }
}
