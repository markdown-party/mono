package party.markdown.tree

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber.Companion.Min
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import party.markdown.tree.TreeEvent.NewFile
import party.markdown.tree.utils.TreeAggregate
import party.markdown.tree.utils.tree

class MutableTreeNoMoveTest {

  @Test
  fun createFileUnnamed() {
    val alice = SiteIdentifier.Min
    val t1 = EventIdentifier(Min, alice)
    val e1 = NewFile

    val expected = tree { file(t1) }

    TreeAggregate.permutations(
        t1 to e1,
    ) { assertEquals(expected, this) }
  }
}
