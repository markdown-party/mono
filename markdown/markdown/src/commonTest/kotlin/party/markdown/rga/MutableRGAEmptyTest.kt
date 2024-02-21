package party.markdown.rga

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber.Companion.Max
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber.Companion.Min
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import party.markdown.rga.utils.RGAAggregate
import kotlin.test.Test
import kotlin.test.assertTrue

class MutableRGAEmptyTest {

  @Test
  fun noOp() =
      with(RGAAggregate()) {
        test {
          // Obviously :-)
          assertTrue(isEmpty())
        }
      }

  @Test
  fun removeNonExisting_one() =
      with(RGAAggregate()) {
        val alice = SiteIdentifier.Min
        val bob = SiteIdentifier.Max
        val offset = EventIdentifier(Min, bob)
        event(Min, alice, RGAEvent.Remove(offset))
        test { assertTrue(isEmpty()) }
      }

  @Test
  fun removeNonExisting_one_repeated() =
      with(RGAAggregate()) {
        val alice = SiteIdentifier.Min
        val bob = SiteIdentifier.Max
        val offset = EventIdentifier(Min, bob)
        repeat(10) { event(Min, alice, RGAEvent.Remove(offset)) }
        test { assertTrue(isEmpty()) }
      }

  @Test
  fun removeNonExisting_multi() =
      with(RGAAggregate()) {
        val alice = SiteIdentifier.Min
        val bob = SiteIdentifier.Max
        val offset1 = EventIdentifier(Max, alice)
        val offset2 = EventIdentifier(Max, bob)
        event(Min, alice, RGAEvent.Remove(offset2))
        event(Min, bob, RGAEvent.Remove(offset1))
        test { assertTrue(isEmpty()) }
      }
}
