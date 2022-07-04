package party.markdown.rga.utils

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import party.markdown.rga.MutableRGA
import party.markdown.rga.RGAEvent
import party.markdown.rga.RGAProjection
import party.markdown.utils.permutations

/**
 * A simple aggregate of events that can be used to write some unit tests of the RGA replicated data
 * type.
 *
 * Standard usage looks as follows :
 *
 * ```kotlin
 * with(RGAAggregate()) {
 *   event(RGAEvent.Remove(EventIdentifier.Unspecified))
 *   test { assertTrue(isEmpty()) }
 * }
 * ```
 */
class RGAAggregate {

  private var current = MutableRGA()

  /**
   * Pushes a [RGAEvent] to the aggregate, applying it immediately.
   *
   * @param seqno the [SequenceNumber] for the event.
   * @param site the [SiteIdentifier] for the event.
   * @param event the [RGAEvent] that is pushed.
   */
  fun event(
      seqno: SequenceNumber,
      site: SiteIdentifier,
      event: RGAEvent,
  ) {
    current = RGAProjection.forward(current, EventIdentifier(seqno, site), event)
  }

  /**
   * Pushes a [RGAEvent] to the aggregate, applying it immediately.
   *
   * @param identifier the [EventIdentifier] for the event.
   * @param event the [RGAEvent] that is pushed.
   */
  fun event(
      identifier: EventIdentifier,
      event: RGAEvent,
  ) {
    event(identifier.seqno, identifier.site, event)
  }

  /**
   * Creates the aggregated [CharArray] of the RGA, so conditions may be tested on it.
   *
   * @param block the function in which you should make assertions over the RGA.
   */
  fun test(block: CharArray.() -> Unit): Unit = block(current.toCharArray())

  companion object {

    /**
     * Tests that the all the permutations of the given events satisfy some conditions. The test
     * [block] will therefore be executed `events!` times.
     *
     * @param events the events that will be permuted.
     * @param block the test block.
     */
    fun permutations(
        vararg events: Pair<EventIdentifier, RGAEvent>,
        block: CharArray.(List<Pair<EventIdentifier, RGAEvent>>) -> Unit,
    ) {
      for (perm in events.toList().permutations()) {
        with(RGAAggregate()) {
          for ((id, event) in perm) {
            event(id.seqno, id.site, event)
          }
          test { block(perm) }
        }
      }
    }
  }
}
