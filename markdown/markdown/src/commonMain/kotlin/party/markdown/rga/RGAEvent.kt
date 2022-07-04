package party.markdown.rga

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A [RGANodeIdentifier] identifies an atom in an RGA sequence. All atoms have a distinct
 * [RGANodeIdentifier].
 */
typealias RGANodeIdentifier = EventIdentifier

// TODO : Use the document id as the RGANodeIdentifier offset.

/**
 * The root [RGANodeIdentifier], which indicates that an [RGAEvent.Insert] takes place at the
 * beginning of the text sequence.
 */
val RGANodeRoot = RGANodeIdentifier.Unspecified

/**
 * An [RGAEvent] defines the two operations which are possible on a replicated growable array :
 * insertions of new atoms, and removals of existing atoms.
 *
 * These operations operate on an `offset`, which references an existing atom position in the
 * sequence. Insertions add a new atom after the `offset`, and removals hide the atom with the given
 * `offset`.
 */
@SerialName("r")
@Serializable
sealed class RGAEvent {

  /**
   * Inserts an atom after the given [RGANodeIdentifier]. If the atom was to be inserted at the
   * beginning of the sequence, the [RGANodeRoot] identifier should be used.
   *
   * @param offset the [RGANodeIdentifier] that we're inserting after.
   * @param atom the [Char] that is inserted.
   */
  @Serializable
  @SerialName("r:i")
  data class Insert(
      val offset: RGANodeIdentifier,
      val atom: Char,
  ) : RGAEvent()

  /**
   * Removes the atom with the given [offset] from the data structure. More specifically, this node
   * will be marked with a tombstone.
   *
   * @param offset the [RGANodeIdentifier] that indicates which node was deleted.
   */
  @Serializable
  @SerialName("r:r")
  data class Remove(
      val offset: RGANodeIdentifier,
  ) : RGAEvent()
}
