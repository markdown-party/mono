package party.markdown.cursors

import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlinx.datetime.Instant
import party.markdown.tree.TreeNodeIdentifier

/**
 * A class representing the [Cursors] which are currently available to application. The set of
 * available cursors will be available on a per-document basis.
 */
class Cursors(mutable: MutableCursors) {

  /**
   * A data class representing a [Cursor], at the index at which it is available.
   *
   * @param actor the [SiteIdentifier] for this specific cursor.
   * @param timestamp the [Instant] at which the cursor was placed.
   * @param anchor the [CursorAnchorIdentifier] where the cursor is located.
   */
  data class Cursor(
      val actor: SiteIdentifier,
      val timestamp: Instant,
      val anchor: CursorAnchorIdentifier,
  )

  /** The [MutableMap] of the [MutableSet] of cursors, for each site. */
  private val byNode = mutableMapOf<TreeNodeIdentifier, MutableSet<Cursor>>()

  init {
    for (i in 0 until mutable.ids.size) {
      val id = mutable.ids[i]
      val anchor = mutable.anchors[i]
      val node = mutable.nodes[i]
      val timestamp = mutable.localTimestamp[i]
      byNode
          .getOrPut(node) { mutableSetOf() }
          .add(
              Cursor(
                  actor = id.site,
                  timestamp = timestamp,
                  anchor = anchor,
              ))
    }
  }

  /** Returns the [Set] of all the [Cursor] available for a given [node]. */
  operator fun get(
      node: TreeNodeIdentifier,
  ): Set<Cursor> = byNode[node] ?: emptySet()
}
