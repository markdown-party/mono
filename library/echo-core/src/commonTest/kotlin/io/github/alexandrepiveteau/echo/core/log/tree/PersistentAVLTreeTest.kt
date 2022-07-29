package io.github.alexandrepiveteau.echo.core.log.tree

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentAVLTreeTest {

  @Test
  fun emptyTree_containsReturnsFalse() {
    val tree = PersistentAVLTree<Int>()
    assertFalse(0 in tree)
  }

  @Test
  fun singletonTree_containsReturnsTrueForElement() {
    var tree = PersistentAVLTree<Int>()
    tree += 42
    assertTrue(42 in tree)
    assertFalse(43 in tree)
  }

  @Test
  fun duplicateInsertions_containsReturnsElement() {
    var tree = PersistentAVLTree<Int>()
    tree += 42
    tree += 42
    assertTrue(42 in tree)
    // TODO : Assertions on the tree size.
  }

  @Test
  fun multipleInsertions_containsAllElements() {
    var tree = PersistentAVLTree<Int>()
    tree += 1
    tree += 2
    tree += 3
    assertTrue(setOf(1, 2, 3).all { it in tree })
  }

  @Test
  fun manyInsertions_containsAllElements() {
    val count = 2048
    var tree = PersistentAVLTree<Int>()
    repeat(count) { tree += it }
    repeat(count) { assertTrue(it in tree) }
  }

  @Test
  fun insertionThenRemoval_isEmpty() {
    var tree = PersistentAVLTree<Int>()
    tree += 1
    tree -= 1
    assertFalse(1 in tree)
  }

  @Test
  fun manyInsertionsManyRemovals_containsSomeElements() {
    val count = 2048
    var tree = PersistentAVLTree<Int>()
    repeat(count) { tree += it }
    repeat(count / 2) { tree -= 2 * it } // Remove all the even numbers.
    repeat(count / 2) { assertFalse(2 * it in tree) } // All event numbers are out.
    repeat(count / 2) { assertTrue(2 * it + 1 in tree) } // All odd numbers are in.
  }

  // TODO : Test all kinds of rotations. Have a traversal method in the tree.
}
