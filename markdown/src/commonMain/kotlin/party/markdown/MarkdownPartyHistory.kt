package party.markdown

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.isUnspecified
import io.github.alexandrepiveteau.echo.core.log.AbstractMutableHistory
import io.github.alexandrepiveteau.echo.projections.TwoWayMutableProjection
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoBuf.Default.decodeFromByteArray
import kotlinx.serialization.serializer
import party.markdown.MarkdownPartyEvent.*
import party.markdown.rga.RGAEvent.Insert
import party.markdown.rga.RGAEvent.Remove

/**
 * A [TwoWayMutableProjection] that wraps the [MarkdownPartyProjection] with the proper change and
 * event [serializer], and stores the events in [ProtoBuf].
 */
private val Projection =
    TwoWayMutableProjection(
        projection = MarkdownPartyProjection,
        changeSerializer = serializer(),
        eventSerializer = serializer(),
        format = ProtoBuf,
    )

/**
 * A [MarkdownPartyHistory] manages a [MutableMarkdownParty] through a [MarkdownPartyProjection].
 * Additionally, it features some special optimizations that remove causally stable move operations
 * of the cursors from the history.
 */
class MarkdownPartyHistory :
    AbstractMutableHistory<MutableMarkdownParty>(
        initial = MutableMarkdownParty(),
        projection = Projection,
    ) {

  override fun partialInsert(
      id: EventIdentifier,
      array: ByteArray,
      from: Int,
      until: Int,
  ) {
    val operationArray = array.copyOfRange(from, until)

    // 1. Look at the type of the operation.
    // 2. Query the identifier of the operation to remove from the history.
    // 3. Partially remove the operation if appropriate.
    when (val event = decodeFromByteArray(MarkdownPartyEvent.serializer(), operationArray)) {
      is Cursor -> handleCursor(id)
      is Tree -> Unit
      is RGA ->
          when (event.event) {
            is Insert -> handleCursor(id)
            is Remove -> Unit
          }
    }

    // 4. Insert the operation. Because we know that our operation will only be inserted for a given
    // site if it has a greater sequence number than all the previously inserted operations, we are
    // guaranteed never to have to skip the partial insertion.
    super.partialInsert(id, array, from, until)
  }

  /** Partially removes the previous operation if it is present. */
  private fun handleCursor(id: EventIdentifier) {
    val previous = current.cursors.previous(id)
    if (previous.isUnspecified || previous > id) return // Do not remove that op.
    partialRemove(previous.seqno, previous.site)
  }
}
