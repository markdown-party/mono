package party.markdown.tree.utils

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.mutableHistoryOf
import io.github.alexandrepiveteau.echo.projections.TwoWayMutableProjection
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import party.markdown.tree.*
import party.markdown.utils.permutations

/**
 * A simple aggregate of events that can be used to write some unit tests of the replicated tree
 * data type. Internally, it makes use of a [mutableHistoryOf] and the empty [MutableTree] model,
 * which guarantees proper forward-backward application of events and changes.
 *
 * Standard usage looks as follows :
 *
 * ```kotlin
 * with (TreeAggregate()) {
 *   event(TreeEvent.Something)
 *   test { assertSomething(this) }
 * }
 * ```
 */
class TreeAggregate {

  @OptIn(ExperimentalSerializationApi::class)
  private val history =
      mutableHistoryOf(
          MutableTree(),
          TwoWayMutableProjection(
              TreeProjection,
              TreeChange.serializer(),
              TreeEvent.serializer(),
              ProtoBuf,
          ),
      )

  /**
   * Pushes a [TreeEvent] to the aggregate, applying it immediately.
   *
   * @param seqno the [SequenceNumber] for the event.
   * @param site the [SiteIdentifier] for the event.
   * @param event the [TreeEvent] that is pushed.
   */
  fun event(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: TreeEvent,
  ) {
    val serialized = ProtoBuf.encodeToByteArray(TreeEvent.serializer(), event)
    history.insert(seqno, site, serialized)
  }

  /**
   * Pushes a [TreeEvent] to the aggregate, applying it immediately.
   *
   * @param identifier the [EventIdentifier] for the event.
   * @param event the [TreeEvent] that is pushed.
   */
  fun event(
      identifier: EventIdentifier,
      event: TreeEvent,
  ) {
    event(identifier.seqno, identifier.site, event)
  }

  /**
   * Creates the aggregated tree, so conditions may be tested on it.
   *
   * @param block the function in which you should make assertions over the tree.
   */
  fun test(block: TreeNode.() -> Unit): Unit = block(history.current.toTree())

  companion object {

    /**
     * Tests that all the permutations of the given events satisfy some conditions. The test [block]
     * will therefore be executed `events!` times.
     *
     * @param events the events that will be permuted.
     * @param block the test block.
     */
    fun permutations(
        vararg events: Pair<EventIdentifier, TreeEvent>,
        block: TreeNode.(List<Pair<EventIdentifier, TreeEvent>>) -> Unit,
    ) {
      // Because we're using a mutableSite internally, we have to be extra-cautious here. Indeed,
      // events emitted by a single site may not get reordered, since mutableSite makes the
      // assumption that events from a single site are delivered with incrementing SequenceNumber.
      //
      // Therefore, we store the list of all the site identifiers. If the permutation is valid,
      // we'll test it.

      val sites = events.asSequence().map { it.first.site }.toSet()

      for (perm in events.toList().permutations()) {
        val respectsOrder =
            sites.all { s ->
              val permF = perm.filter { it.first.site == s }
              val eventsF = events.filter { it.first.site == s }
              permF == eventsF
            }
        if (respectsOrder) {
          with(TreeAggregate()) {
            for ((id, event) in perm) {
              event(id.seqno, id.site, event)
            }
            test { block(perm) }
          }
        }
      }
    }
  }
}
