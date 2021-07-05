package party.markdown.rga

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber.Companion.Min
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import party.markdown.rga.utils.RGAAggregate

class MutableRGASingleDeliveryTest {

  @Test
  fun consecutiveInsertions() {
    val alice = Random.nextSiteIdentifier()
    val t1 = EventIdentifier(Min + 0u, alice)
    val t2 = EventIdentifier(Min + 1u, alice)
    val t3 = EventIdentifier(Min + 2u, alice)

    val e1 = RGAEvent.Insert(RGANodeRoot, 'a')
    val e2 = RGAEvent.Insert(t1, 'b')
    val e3 = RGAEvent.Insert(t2, 'c')

    // Test all permutations.
    RGAAggregate.permutations(
        t1 to e1,
        t2 to e2,
        t3 to e3,
    ) { assertContentEquals(charArrayOf('a', 'b', 'c'), this, "Permutation $it") }
  }

  @Test
  fun insertionThenRemoval() {
    val alice = Random.nextSiteIdentifier()
    val t1 = EventIdentifier(Min + 0u, alice)
    val t2 = EventIdentifier(Min + 1u, alice)

    val e1 = RGAEvent.Insert(RGANodeRoot, 'a')
    val e2 = RGAEvent.Remove(t1)

    // Test all permutations.
    RGAAggregate.permutations(
        t1 to e1,
        t2 to e2,
    ) { assertContentEquals(charArrayOf(), this, "Permutation $it") }
  }

  @Test
  fun consecutiveInsertionsAtStart() {
    val alice = Random.nextSiteIdentifier()
    val t1 = EventIdentifier(Min + 0u, alice)
    val t2 = EventIdentifier(Min + 1u, alice)
    val t3 = EventIdentifier(Min + 2u, alice)

    val e1 = RGAEvent.Insert(RGANodeRoot, 'a')
    val e2 = RGAEvent.Insert(RGANodeRoot, 'b')
    val e3 = RGAEvent.Insert(RGANodeRoot, 'c')

    // Test all permutations.
    RGAAggregate.permutations(
        t1 to e1,
        t2 to e2,
        t3 to e3,
    ) { assertContentEquals(charArrayOf('c', 'b', 'a'), this, "Permutation $it") }
  }

  @Test
  fun insertionOnRemovedChar() {
    val alice = 1U.toSiteIdentifier()
    val bob = 2U.toSiteIdentifier()
    val t1 = EventIdentifier(Min + 0u, alice)
    val t2 = EventIdentifier(Min + 1u, alice)
    val t3 = EventIdentifier(Min + 1u, bob)
    val t4 = EventIdentifier(Min + 2u, alice)

    val e1 = RGAEvent.Insert(RGANodeRoot, 'a')
    val e2 = RGAEvent.Remove(t1)
    val e3 = RGAEvent.Insert(t1, 'b')
    val e4 = RGAEvent.Insert(t1, 'c')

    // Test all permutations.
    RGAAggregate.permutations(
        t1 to e1,
        t2 to e2,
        t3 to e3,
        t4 to e4,
    ) { assertContentEquals(charArrayOf('c', 'b'), this, "Permutation $it") }
  }

  @Test
  fun crossRemovals() {
    val alice = 1U.toSiteIdentifier()
    val bob = 2U.toSiteIdentifier()
    val t1 = EventIdentifier(Min + 0u, alice)
    val t2 = EventIdentifier(Min + 0u, bob)
    val t3 = EventIdentifier(Min + 1u, alice)
    val t4 = EventIdentifier(Min + 1u, bob)

    val e1 = RGAEvent.Insert(RGANodeRoot, 'a')
    val e2 = RGAEvent.Insert(RGANodeRoot, 'b')
    val e3 = RGAEvent.Remove(t1)
    val e4 = RGAEvent.Remove(t2)

    // Test all permutations.
    RGAAggregate.permutations(
        t1 to e1,
        t2 to e2,
        t3 to e3,
        t4 to e4,
    ) { assertContentEquals(charArrayOf(), this, "Permutation $it") }
  }

  @Test
  fun manyConsecutiveInsertions() {
    val count = 8 // 8! = 40'320 inter-leavings to be tested.
    val expected = CharArray(count) { it.digitToChar() }

    val alice = SiteIdentifier.Min
    val events = mutableListOf<Pair<EventIdentifier, RGAEvent>>()
    repeat(count) {
      val t = EventIdentifier(Min + it.toUInt(), alice)
      val prev = if (it == 0) RGANodeRoot else EventIdentifier(Min + (it - 1).toUInt(), alice)
      val e = RGAEvent.Insert(prev, it.digitToChar())
      events.add(t to e)
    }

    // Test all permutations.
    RGAAggregate.permutations(*events.toTypedArray()) {
      assertContentEquals(expected, this, "Permutation $it")
    }
  }
}
