package party.markdown.rga

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import party.markdown.rga.utils.RGAAggregate

class MutableRGAMultiDeliveryTest {

  @Test
  fun duplicateInsertions() {
    val alice = Random.nextSiteIdentifier()
    val t1 = EventIdentifier(SequenceNumber.Min + 0u, alice)
    val t2 = EventIdentifier(SequenceNumber.Min + 1u, alice)

    val e1 = RGAEvent.Insert(RGANodeRoot, 'a')
    val e2 = RGAEvent.Insert(t1, 'b')

    // Test all permutations.
    RGAAggregate.permutations(
        t1 to e1,
        t1 to e1,
        t2 to e2,
        t2 to e2,
    ) { assertContentEquals(charArrayOf('a', 'b'), this, "Permutation $it") }
  }

  @Test
  fun duplicateRemovals() {
    val alice = Random.nextSiteIdentifier()
    val t1 = EventIdentifier(SequenceNumber.Min + 0u, alice)
    val t2 = EventIdentifier(SequenceNumber.Min + 1u, alice)

    val e1 = RGAEvent.Insert(RGANodeRoot, 'a')
    val e2 = RGAEvent.Remove(t1)

    // Test all permutations.
    RGAAggregate.permutations(
        t1 to e1,
        t2 to e2,
        t2 to e2,
    ) { assertContentEquals(charArrayOf(), this, "Permutation $it") }
  }
}
