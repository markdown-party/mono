package io.github.alexandrepiveteau.echo.core.log.tree

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AVLTreeTest {

  @Test
  fun emptyTree_containsReturnsFalse() {
    val tree = AVLTree<Int>()
    assertFalse(0 in tree)
  }

  @Test
  fun singletonTree_containsReturnsTrueForElement() {
    val tree = AVLTree<Int>()
    tree.insert(42)
    assertTrue(42 in tree)
    assertFalse(43 in tree)
  }

  @Test
  fun duplicateInsertions_containsReturnsElement() {
    val tree = AVLTree<Int>()
    tree.insert(42)
    tree.insert(42)
    assertTrue(42 in tree)
    // TODO : Assertions on the tree size.
  }

  @Test
  fun multipleInsertions_containsAllElements() {
    val tree = AVLTree<Int>()
    tree.insert(1)
    tree.insert(2)
    tree.insert(3)
    assertTrue(setOf(1, 2, 3).all { it in tree })
  }
}
