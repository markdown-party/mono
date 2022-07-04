package party.markdown.rga

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber.Companion.Min
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlin.test.Test
import kotlin.test.assertContentEquals
import party.markdown.rga.utils.RGAAggregate

class MutableRGAInterleavingTest {

  @Test
  fun consecutiveInsertions() {
    val alice = SiteIdentifier.Min
    val bob = SiteIdentifier.Max

    val t1 = EventIdentifier(Min + 0u, alice)
    val t2 = EventIdentifier(Min + 1u, alice)
    val t3 = EventIdentifier(Min + 2u, alice)
    val t4 = EventIdentifier(Min + 1u, bob)
    val t5 = EventIdentifier(Min + 2u, bob)

    val e1 = RGAEvent.Insert(RGANodeRoot, 'X')
    val e2 = RGAEvent.Insert(t1, 'A')
    val e3 = RGAEvent.Insert(t2, 'B')
    val e4 = RGAEvent.Insert(t3, 'C')
    val e5 = RGAEvent.Insert(t4, 'D')

    RGAAggregate.permutations(
        t1 to e1,
        t2 to e2,
        t3 to e3,
        t4 to e4,
        t5 to e5,
    ) { assertContentEquals(charArrayOf('X', 'A', 'B', 'C', 'D'), this, "Permutation $it") }
  }
}
