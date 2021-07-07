package party.markdown.tree

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber.Companion.Min
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import party.markdown.tree.TreeEvent.*
import party.markdown.tree.utils.TreeAggregate
import party.markdown.tree.utils.tree

class MutableTreeNoSkipTest {

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

  @Test
  fun createFileNamed() {
    val alice = SiteIdentifier.Min
    val t1 = EventIdentifier(Min + 0u, alice)
    val t2 = EventIdentifier(Min + 1u, alice)
    val e1 = NewFile
    val e2 = Name(t1, "File")

    val expected = tree { file(t1, "File") }

    TreeAggregate.permutations(
        t1 to e1,
        t2 to e2,
    ) { assertEquals(expected, this, "Permutation $it") }
  }

  @Test
  fun createRemoveFile() {
    val alice = SiteIdentifier.Min
    val bob = SiteIdentifier.Max
    val t1 = EventIdentifier(Min + 0u, alice)
    val t2 = EventIdentifier(Min + 1u, bob)
    val expected = tree {}
    TreeAggregate.permutations(
        t1 to NewFile,
        t2 to Remove(t1),
    ) { assertEquals(expected, this, "Permutation $it") }
  }

  @Test
  fun createMultipleChildren() {
    val alice = SiteIdentifier.Min
    val bob = SiteIdentifier.Max
    val t1 = EventIdentifier(Min + 0u, alice)
    val t2 = EventIdentifier(Min + 1u, alice)
    val t3 = EventIdentifier(Min + 1u, bob)
    val t4 = EventIdentifier(Min + 2u, alice)
    val t5 = EventIdentifier(Min + 3u, alice)
    val expected = tree {
      file(t1)
      file(t2)
      folder(t3) { file(t4) }
    }
    TreeAggregate.permutations(
        t1 to NewFile,
        t2 to NewFile,
        t3 to NewFolder,
        t4 to NewFile,
        t5 to Move(t4, t3),
    ) { assertEquals(expected, this, "Permutation $it") }
  }

  @Test
  fun deleteFolder() {
    val alice = SiteIdentifier.Min
    val bob = SiteIdentifier.Max
    val t1 = EventIdentifier(Min + 0u, alice)
    val t2 = EventIdentifier(Min + 1u, alice)
    val t3 = EventIdentifier(Min + 2u, alice)
    val t4 = EventIdentifier(Min + 2u, bob)
    val expected = tree {}
    TreeAggregate.permutations(
        t1 to NewFolder,
        t2 to NewFile,
        t3 to Move(t2, t1),
        t4 to Remove(t1),
    ) { assertEquals(expected, this, "Permutation $it") }
  }

  @Test
  fun concurrentMove() {
    val alice = SiteIdentifier.Min
    val bob = SiteIdentifier.Max
    val t1 = EventIdentifier(Min + 0u, alice)
    val t2 = EventIdentifier(Min + 0u, bob)
    val t3 = EventIdentifier(Min + 1u, alice)
    val t4 = EventIdentifier(Min + 2u, alice)
    val t5 = EventIdentifier(Min + 2u, bob)
    val expected = tree { folder(t1) { folder(t2) { file(t3) } } }
    TreeAggregate.permutations(
        t1 to NewFolder,
        t2 to NewFolder,
        t3 to NewFile,
        t4 to Move(t2, t1),
        t5 to Move(t3, t2),
    ) { assertEquals(expected, this, "Permutation $it") }
  }
}
