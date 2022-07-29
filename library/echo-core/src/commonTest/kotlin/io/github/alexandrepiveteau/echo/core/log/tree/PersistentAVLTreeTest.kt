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
}
