package io.github.alexandrepiveteau.echo.core.log.tree

import io.github.alexandrepiveteau.echo.core.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentAVLTreeTest {

  @Test
  fun emptyTree_containsReturnsFalse() {
    val tree = PersistentAVLTree<Int, Int>()
    assertFalse(0 in tree)
    assertEquals(0, tree.size)
  }

  @Test
  fun singletonTree_containsReturnsTrueForElement() {
    var tree = PersistentAVLTree<Int, Unit>()
    tree += 42 to Unit
    assertTrue(42 in tree)
    assertFalse(43 in tree)
    assertEquals(1, tree.size)
  }

  @Test
  fun duplicateInsertions_containsReturnsElement() {
    var tree = PersistentAVLTree<Int, Unit>()
    tree += 42 to Unit
    tree += 42 to Unit
    assertTrue(42 in tree)
    assertEquals(1, tree.size)
  }

  @Test
  fun multipleInsertions_containsAllElements() {
    var tree = PersistentAVLTree<Int, Unit>()
    tree += 1 to Unit
    tree += 2 to Unit
    tree += 3 to Unit
    assertTrue(setOf(1, 2, 3).all { it in tree })
    assertEquals(3, tree.size)
  }

  @Test
  fun manyInsertions_containsAllElements() {
    val count = 2048
    var tree = PersistentAVLTree<Int, Unit>()
    repeat(count) { tree += it to Unit }
    repeat(count) { assertTrue(it in tree) }
    assertEquals(count, tree.size)
  }

  @Test
  fun insertionThenRemoval_isEmpty() {
    var tree = PersistentAVLTree<Int, Unit>()
    tree += 1 to Unit
    tree -= 1
    assertFalse(1 in tree)
    assertEquals(0, tree.size)
  }

  @Test
  fun manyInsertionsManyRemovals_containsSomeElements() {
    val count = 2048
    var tree = PersistentAVLTree<Int, Unit>()
    repeat(count) { tree += it to Unit }
    repeat(count / 2) { tree -= 2 * it } // Remove all the even numbers.
    repeat(count / 2) { assertFalse(2 * it in tree) } // All event numbers are out.
    repeat(count / 2) { assertTrue(2 * it + 1 in tree) } // All odd numbers are in.
    assertEquals(count / 2, tree.size)
  }

  @Test
  fun manyInsertions_iterator_hasAllElements() {
    val count = 2048
    var tree = PersistentAVLTree<Int, Unit>()
    repeat(count) { tree += it to Unit }
    val iterator = tree.iterator()
    repeat(count) {
      check(iterator.hasNext())
      assertEquals(it, iterator.next().key)
    }
  }

  @Test
  fun manyInsertions_iteratorAtEnd_hasAllElements() {
    val count = 2048
    var tree = PersistentAVLTree<Int, Unit>()
    repeat(count) { tree += it to Unit }
    val iterator = tree.iteratorAtEnd()
    repeat(count) {
      check(iterator.hasPrevious())
      assertEquals(count - it - 1, iterator.previous().key)
    }
  }

  @Test
  fun emptyTree_hasEmptyIterator() {
    val tree = PersistentAVLTree<Nothing, Nothing>()
    val iterator = tree.iterator()
    assertFalse(iterator.hasPrevious())
    assertFalse(iterator.hasNext())
    assertThrows<IllegalStateException> { iterator.previous() }
    assertThrows<IllegalStateException> { iterator.previousIndex() }
    assertThrows<IllegalStateException> { iterator.next() }
    assertThrows<IllegalStateException> { iterator.nextIndex() }
  }

  @Test
  fun emptyTree_hasEmptyIteratorAtEnd() {
    val tree = PersistentAVLTree<Nothing, Nothing>()
    val iterator = tree.iteratorAtEnd()
    assertFalse(iterator.hasPrevious())
    assertFalse(iterator.hasNext())
    assertThrows<IllegalStateException> { iterator.previous() }
    assertThrows<IllegalStateException> { iterator.previousIndex() }
    assertThrows<IllegalStateException> { iterator.next() }
    assertThrows<IllegalStateException> { iterator.nextIndex() }
  }

  @Test
  fun smallTree_iterator_worksInBothDirections() {
    var tree = PersistentAVLTree<Int, Unit>()
    repeat(10) { tree += it to Unit }
    val iterator = tree.iterator()
    assertFalse(iterator.hasPrevious())
    assertTrue(iterator.hasNext())
    assertEquals(0, iterator.next().key)
    assertTrue(iterator.hasPrevious())
    assertEquals(0, iterator.previous().key)
    assertFalse(iterator.hasPrevious())
    assertTrue(iterator.hasNext())
  }
}
