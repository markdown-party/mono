package party.markdown

import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.*
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
 * Returns a [MutableHistory] which manages a [MutableMarkdownParty] through a
 * [MarkdownPartyProjection]. Additionally, it features some special optimizations that remove
 * causally stable move operations of the cursors from the history.
 */
@Suppress("FunctionName")
fun MarkdownPartyHistory(): MutableHistory<MutableMarkdownParty> {
  val history = mutableHistoryOf(initial = MutableMarkdownParty(), projection = Projection)
  history.registerLogUpdateListener(MarkdownPartyLogUpdateListener(history))
  return history
}

/**
 * An implementation of [EventLog.OnLogUpdateListener] which compressed cursor operations for a
 * [MutableHistory] of [MutableMarkdownParty].
 *
 * @param history the underlying [MutableHistory].
 */
private class MarkdownPartyLogUpdateListener(
    private val history: MutableHistory<MutableMarkdownParty>,
) : EventLog.OnLogUpdateListener {

  override fun onInsert(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      data: ByteArray,
      from: Int,
      until: Int
  ) {
    val operationArray = data.copyOfRange(from, until)

    // 1. Look at the type of the operation.
    // 2. Query the identifier of the operation to remove from the history.
    // 3. Partially remove the operation if appropriate.
    when (val event = decodeFromByteArray(MarkdownPartyEvent.serializer(), operationArray)) {
      is Cursor -> handleCursor()
      is Tree -> Unit
      is RGA ->
          when (event.event) {
            is Insert -> handleCursor()
            is Remove -> Unit
          }
    }
  }

  /** Compacts the previous operation cursor moves if it is present. */
  private fun handleCursor() {
    val candidates = history.current.cursors.compactionCandidates
    for (index in 0 until candidates.size) {
      val candidate = candidates[index]
      history.remove(candidate.seqno, candidate.site)
    }
    candidates.clear()
  }
}
