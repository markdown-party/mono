package party.markdown.tree

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import party.markdown.tree.TreeEvent.Move
import party.markdown.tree.TreeEvent.NewFolder
import party.markdown.tree.utils.TreeAggregate
import party.markdown.tree.utils.tree

class MutableTreeSkipTest {

  @Test
  fun moveIntoSelf() {
    val alice = SiteIdentifier.Min
    val t1 = EventIdentifier(SequenceNumber.Min + 0u, alice)
    val t2 = EventIdentifier(SequenceNumber.Min + 1u, alice)
    val expected = tree { folder(t1) {} }
    TreeAggregate.permutations(
        t1 to NewFolder,
        t2 to Move(t1, t1), // skipped
    ) { assertEquals(expected, this, "Permutation $it") }
  }

  @Test
  fun loop() {
    val alice = SiteIdentifier.Min
    val bob = SiteIdentifier.Max
    val t1 = EventIdentifier(SequenceNumber.Min + 0u, alice)
    val t2 = EventIdentifier(SequenceNumber.Min + 1u, alice)
    val t3 = EventIdentifier(SequenceNumber.Min + 2u, alice)
    val t4 = EventIdentifier(SequenceNumber.Min + 2u, bob)
    val expected = tree { folder(t1) { folder(t2) {} } }
    TreeAggregate.permutations(
        t1 to NewFolder,
        t2 to NewFolder,
        t3 to Move(t2, t1),
        t4 to Move(t1, t2), // skipped
    ) { assertEquals(expected, this, "Permutation $it") }
  }

  @Test
  fun loop3() {
    val alice = 1u.toSiteIdentifier()
    val bob = 2u.toSiteIdentifier()
    val charlie = 3u.toSiteIdentifier()
    val t1 = EventIdentifier(SequenceNumber.Min + 0u, alice)
    val t2 = EventIdentifier(SequenceNumber.Min + 0u, bob)
    val t3 = EventIdentifier(SequenceNumber.Min + 0u, charlie)
    val t4 = EventIdentifier(SequenceNumber.Min + 1u, alice)
    val t5 = EventIdentifier(SequenceNumber.Min + 1u, bob)
    val t6 = EventIdentifier(SequenceNumber.Min + 1u, charlie)
    val expected = tree { folder(t1) { folder(t2) { folder(t3) {} } } }
    TreeAggregate.permutations(
        t1 to NewFolder,
        t2 to NewFolder,
        t3 to NewFolder,
        t4 to Move(t2, t1),
        t5 to Move(t3, t2),
        t6 to Move(t1, t3), // skipped
    ) { assertEquals(expected, this, "Permutation $it") }
  }

  @Test
  fun loopThenOk() {
    val alice = SiteIdentifier.Min
    val bob = SiteIdentifier.Max
    val t1 = EventIdentifier(SequenceNumber.Min + 0u, alice)
    val t2 = EventIdentifier(SequenceNumber.Min + 1u, alice)
    val t3 = EventIdentifier(SequenceNumber.Min + 2u, alice)
    val t4 = EventIdentifier(SequenceNumber.Min + 2u, bob)
    val t5 = EventIdentifier(SequenceNumber.Min + 3u, bob)
    val expected = tree {
      folder(t1) {}
      folder(t2) {}
    }
    TreeAggregate.permutations(
        t1 to NewFolder,
        t2 to NewFolder,
        t3 to Move(t2, t1),
        t4 to Move(t1, t2), // skipped
        t5 to Move(t2, TreeNodeRoot),
    ) { assertEquals(expected, this, "Permutation $it") }
  }
}
