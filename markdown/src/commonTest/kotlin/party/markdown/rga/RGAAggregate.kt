package party.markdown.rga

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier

/**
 * A simple aggregate of events that can be used to write some unit tests of the RGA replicated data
 * type.
 *
 * Standard usage looks as follows :
 *
 * ```kotlin
 * with(RGAAggregate) {
 *   event(RGAEvent.Remove(EventIdentifier.Unspecified))
 *   test { assertTrue(isEmpty()) }
 * }
 * ```
 */
class RGAAggregate {

  private var current = MutableRGA()
  private val projection = RGAProjection()

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
    current = projection.forward(current, EventIdentifier(seqno, site), event)
  }

  /**
   * Creates the aggregated [CharArray] of the RGA, so conditions may be tested on it.
   *
   * @param block the function in which you should make assertions over the RGA.
   */
  fun test(block: CharArray.() -> Unit): Unit = block(current.toCharArray())
}
