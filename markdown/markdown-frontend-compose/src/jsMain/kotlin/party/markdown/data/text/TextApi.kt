package party.markdown.data.text

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import kotlinx.coroutines.flow.StateFlow
import party.markdown.cursors.CursorEvent
import party.markdown.cursors.Cursors
import party.markdown.rga.RGAEvent
import party.markdown.tree.TreeNodeIdentifier

/** An interface defining the API to interact with a specific document. */
interface TextApi {

  /**
   * Returns the [StateFlow] of [CharArray], [EventIdentifierArray] and the [Set] of
   * [Cursors.Cursor] for the document with the given [id].
   */
  fun current(
      id: TreeNodeIdentifier,
  ): StateFlow<Triple<CharArray, EventIdentifierArray, Set<Cursors.Cursor>>>

  /**
   * Emits some [RGAEvent] for the replicated growable array associated with a certain
   * [TreeNodeIdentifier]. The changes yielded in the [scope] will be emitted atomically.
   *
   * @param id the [TreeNodeIdentifier] that corresponds to a document.
   * @param scope the [TextCursorEventScope] where we may emit some events.
   */
  suspend fun edit(id: TreeNodeIdentifier, scope: suspend TextCursorEventScope.() -> Unit)
}

/** A scope which lets the consumer yield some [RGAEvent] or some [CursorEvent]. */
interface TextCursorEventScope {

  /** Yields an [RGAEvent], and returns its associated [EventIdentifier]. */
  fun yield(event: RGAEvent): EventIdentifier

  /** Yields a [CursorEvent], and returns its associated [EventIdentifier]. */
  fun yield(event: CursorEvent): EventIdentifier
}
