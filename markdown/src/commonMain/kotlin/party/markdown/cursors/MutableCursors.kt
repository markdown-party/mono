package party.markdown.cursors

import io.github.alexandrepiveteau.echo.core.buffer.binarySearchBySite
import io.github.alexandrepiveteau.echo.core.buffer.mutableEventIdentifierGapBufferOf
import io.github.alexandrepiveteau.echo.core.buffer.mutableGapBufferOf
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import party.markdown.tree.TreeNodeIdentifier

/**
 * A class representing the mutable state of the cursors in the application. More specifically, each
 * site which has issued a move operation for cursors or has inserted a new character will get a
 * single cursor, which will then be displayed across the documents as they move.
 */
class MutableCursors {

  /**
   * The [EventIdentifier] associated with the latest cursor event for each site. This buffer is
   * sorted by [io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier].
   */
  internal val ids = mutableEventIdentifierGapBufferOf()

  /** The tree node identifiers for each event from [ids]. */
  internal val nodes = mutableEventIdentifierGapBufferOf()

  /** The anchor identifiers for each event from [ids]. */
  internal val anchors = mutableEventIdentifierGapBufferOf()

  /** The local physical timestamps for each event from [ids]. */
  internal val localTimestamp = mutableGapBufferOf<Instant>()

  /** `true` if the operation referenced in ids could be compacted. */
  private val shouldCompact = mutableGapBufferOf<Boolean>() // TODO : Specialize MutableGapBuffer

  /**
   * Returns the previous [EventIdentifier] for an [EventIdentifier] that would have resulted in a
   * [put], but only if the operation allows for the compaction of operations.
   */
  internal fun previous(id: EventIdentifier): EventIdentifier {
    val index = ids.binarySearchBySite(id.site)
    if (index >= 0 && ids[index] < id && shouldCompact[index]) return ids[index]
    return EventIdentifier.Unspecified
  }

  /**
   * Puts the cursor at a given [anchor] for a given [node], if its [id] is more recent than the
   * highest identifier received for this site yet.
   *
   * This operation is idempotent, commutative and associative, since it uses the maximum sequence
   * number for the site to determine which event to use.
   */
  private fun put(
      id: EventIdentifier,
      node: TreeNodeIdentifier,
      anchor: CursorAnchorIdentifier,
      compact: Boolean,
  ) {
    val index = ids.binarySearchBySite(id.site)
    if (index < 0) {
      // Insert the move for the cursor, since this site is not known (yet) by the data structure.
      // Additionally, we'll store the local timestamp at which the operation was inserted, so we
      // can hide the cursor after some time in the visual editor.
      //
      // We can simply perform the insertion at the inverted insertion point.
      val insertion = -(index + 1)
      ids.push(id, offset = insertion)
      nodes.push(node, offset = insertion)
      anchors.push(anchor, offset = insertion)
      localTimestamp.push(Clock.System.now(), offset = insertion)
      shouldCompact.push(compact, offset = insertion)
    } else {
      // We have already received some cursor events for this site. We'll compare the sequence
      // numbers of the events, and if we have a more recent event, we'll update the anchor, node
      // and localTimestamp fields.
      if (ids[index].seqno >= id.seqno) return
      ids[index] = id
      nodes[index] = node
      anchors[index] = anchor
      localTimestamp[index] = Clock.System.now()
      shouldCompact[index] = compact
    }
  }

  /**
   * Moves the cursor to the given [anchor], for the provided [node]. This should only be called for
   * operations with an [id] which may be collapsed.
   */
  fun move(
      id: EventIdentifier,
      node: TreeNodeIdentifier,
      anchor: CursorAnchorIdentifier,
  ): Unit = put(id, node, anchor, compact = true)

  /**
   * Moves the cursor after the given [anchor], for the provided [node]. The operation will not be
   * compacted if it becomes stale due to a follow-up [move] or [insert].
   */
  fun insert(
      id: EventIdentifier,
      node: TreeNodeIdentifier,
      anchor: CursorAnchorIdentifier,
  ): Unit = put(id, node, anchor, compact = false)

  /** Returns an immutable [Cursors] data structure, which provides access to all the cursors. */
  fun toCursors(): Cursors = Cursors(this)
}
