package party.markdown.ui.editor

import codemirror.state.*
import io.github.alexandrepiveteau.echo.core.buffer.toEventIdentifierArray
import io.github.alexandrepiveteau.echo.core.buffer.toMutableGapBuffer
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import io.github.alexandrepiveteau.echo.core.causality.isSpecified

/**
 * The [RGAState] contains an [EventIdentifierArray], where each character is individually mapped to
 * an [EventIdentifier] using its position. Characters which have only been appended locally but not
 * synced will be set to [EventIdentifier.Unspecified].
 */
data class RGAState(
    val identifiers: EventIdentifierArray,
    val removed: EventIdentifierArray,
)

/**
 * A [StateField] which contains the current identifiers associated with each character from the
 * state of the RGA.
 */
val RGAStateField = StateField.define(StateFieldConfig(create = ::create, update = ::update))

/**
 * An [Annotation] which indicates what the identifiers of the final text are. This annotation is
 * required for all remote transactions.
 */
val RGAIdentifiers = Annotation.define<EventIdentifierArray>()

/**
 * Creates a new [RGAState] that can be accessed and managed in a lock-in fashion by the CodeMirror
 * editor.
 *
 * @param state the [EditorState] that is applied originally.
 */
private fun create(
    state: EditorState,
): RGAState {
  // By default, the characters are unknown when they are inserted. This means that they will all
  // get appended to the MutableHistory on the next sync loop.
  return RGAState(EventIdentifierArray(state.doc.length), EventIdentifierArray(0))
}

private fun update(
    current: RGAState,
    transaction: Transaction,
): RGAState {
  val remote = transaction.annotation(Transaction.remote)
  if (remote != undefined && remote) {
    // Remote transaction, meaning that we're getting some new characters to insert or some
    // contents to delete.
    val identifiers = transaction.annotation(RGAIdentifiers)
    if (identifiers == undefined) error("Expected annotation $RGAIdentifiers on remote op.")

    // Sanity checks.
    check(identifiers.size == transaction.newDoc.length) { "Mismatching lengths !" }

    // We've found out the identifiers associated with the changes.
    return RGAState(identifiers, EventIdentifierArray(0))
  }

  // Otherwise, we can look at the transaction, its changes, and insert or remove the appropriate
  // missing  event identifiers in the state. This will be picked up later and used to compute the
  // missing insertions that should be performed in the CRDT.

  // Create a MutableGapBuffer, used to perform the insertions, deletions, etc.
  val buffer = current.identifiers.toMutableGapBuffer()
  val removedBuffer = current.removed.toMutableGapBuffer()
  transaction.changes.iterChanges({ fromA, toA, _, _, inserted ->
    // Check that the removed range is non-empty, because if the buffer is empty an
    // OutOfBoundsException will nevertheless be thrown.
    if (toA - fromA > 0) {
      val ids = buffer.remove(fromA, size = toA - fromA)
      for (id in ids) if (id.isSpecified) removedBuffer.push(id)
    }
    repeat(inserted.length) { buffer.push(EventIdentifier.Unspecified, fromA) }
  })

  // Sanity checks.
  check(buffer.size == transaction.newDoc.length) { "Mismatching lengths !" }

  return RGAState(buffer.toEventIdentifierArray(), removedBuffer.toEventIdentifierArray())
}
