package party.markdown.tree

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber.Companion.Min
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import party.markdown.tree.utils.TreeAggregate
import party.markdown.tree.utils.tree

class MutableTreeEmptyTest {

  @Test
  fun noop() {
    with(TreeAggregate()) { test { assertEquals(tree {}, this) } }
  }

  @Test
  fun remove() {
    val alice = SiteIdentifier.Min
    val t1 = EventIdentifier(Min, alice)
    val e1 = TreeEvent.Remove(t1)

    // Test all permutations.
    TreeAggregate.permutations(
        t1 to e1,
    ) { assertEquals(tree {}, this) }
  }

  @Test
  fun name() {
    val alice = SiteIdentifier.Min
    val t1 = EventIdentifier(Min, alice)
    val e1 = TreeEvent.Name(t1, "Hello")

    // Test all permutations.
    TreeAggregate.permutations(
        t1 to e1,
    ) { assertEquals(tree {}, this) }
  }

  @Test
  fun nameRoot() {
    val alice = SiteIdentifier.Min
    val t1 = EventIdentifier(Min, alice)
    val e1 = TreeEvent.Name(TreeNodeRoot, "Hello")

    // Test all permutations.
    TreeAggregate.permutations(
        t1 to e1,
    ) { assertEquals(tree("Hello") {}, this) }
  }
}
