package party.markdown.rga

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import party.markdown.rga.utils.RGAAggregate

class MutableRGASimpleTest {

  @Test
  fun consecutiveInsertions() {
    val alice = Random.nextSiteIdentifier()
    val t1 = EventIdentifier(SequenceNumber.Min + 1u, alice)
    val t2 = EventIdentifier(SequenceNumber.Min + 2u, alice)
    val t3 = EventIdentifier(SequenceNumber.Min + 3u, alice)

    val e1 = RGAEvent.Insert(RGANodeRoot, 'a')
    val e2 = RGAEvent.Insert(t1, 'b')
    val e3 = RGAEvent.Insert(t2, 'c')

    // Test all permutations.
    RGAAggregate.permutations(
        t1 to e1,
        t2 to e2,
        t3 to e3,
    ) { perm -> assertContentEquals(charArrayOf('a', 'b', 'c'), this, "Permutation $perm") }
  }
}
