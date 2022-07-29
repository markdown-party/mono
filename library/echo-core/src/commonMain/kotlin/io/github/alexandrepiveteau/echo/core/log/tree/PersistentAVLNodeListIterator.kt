package io.github.alexandrepiveteau.echo.core.log.tree

import io.github.alexandrepiveteau.echo.core.log.tree.PersistentAVLNodeListIterator.Position.Left
import io.github.alexandrepiveteau.echo.core.log.tree.PersistentAVLNodeListIterator.Position.Right
import io.github.alexandrepiveteau.echo.core.log.tree.PersistentAVLTree.PersistentAVLNode

/**
 * An implementation of [ListIterator] which traverses a tree of [PersistentAVLNode].
 *
 * @param K the type of the keys.
 * @param V the type of the values.
 *
 * @property index the current cursor position.
 * @property stack the recursion stack of the iterator.
 * @property position the [Position] of the cursor relative to the head of the stack.
 */
internal class PersistentAVLNodeListIterator<K : Comparable<K>, out V>
private constructor(
    private var index: Int,
    private val stack: MutableList<PersistentAVLNode<K, V>>,
    private var position: Position,
) : ListIterator<Map.Entry<K, V>> {

  companion object Factory {

    /**
     * Returns a [PersistentAVLNodeListIterator] from the node of the tree rooted at this
     * [PersistentAVLNode] with the smallest key.
     *
     * @param K the type of the keys.
     * @param V the type of the values.
     * @param root the [PersistentAVLNode] for which an iterator is built.
     *
     * @return the [PersistentAVLNodeListIterator].
     */
    fun <K : Comparable<K>, V> fromStart(
        root: PersistentAVLNode<K, V>,
    ): PersistentAVLNodeListIterator<K, V> =
        PersistentAVLNodeListIterator(
            index = 0,
            stack = root.traverseToMin().toMutableList(),
            position = Left,
        )

    /**
     * Returns a [PersistentAVLNodeListIterator] after the node of the tree rooted at this
     * [PersistentAVLNode] with the biggest key.
     *
     * @param K the type of the keys.
     * @param V the type of the values.
     * @param root the [PersistentAVLNode] for which an iterator is built.
     *
     * @return the [PersistentAVLNodeListIterator].
     */
    fun <K : Comparable<K>, V> fromEnd(
        root: PersistentAVLNode<K, V>,
    ): PersistentAVLNodeListIterator<K, V> =
        PersistentAVLNodeListIterator(
            index = root.size - 1,
            stack = root.traverseToMax().toMutableList(),
            position = Right,
        )
  }

  /**
   * An enumeration which indicates the position that the iterator may be in when pointing on the
   * last element of the stack.
   */
  private enum class Position {

    /** Indicates that the cursor is "before" the stack head. */
    Left,

    /** Indicates that the cursor is "after" the stack head. */
    Right,
  }

  override fun hasNext(): Boolean {
    if (position == Left) return true
    if (stack.last().right != null) return true
    return stack.any { it.key > stack.last().key }
  }

  override fun hasPrevious(): Boolean {
    if (position == Right) return true
    if (stack.last().left != null) return true
    return stack.any { it.key < stack.last().key }
  }

  override fun next(): Map.Entry<K, V> {
    check(hasNext())
    when {
      position == Left -> position = Right
      stack.last().right != null -> {
        stack.add(checkNotNull(stack.last().right))
        while (stack.last().left != null) {
          stack.add(checkNotNull(stack.last().left))
        }
      }
      else -> {
        val key = stack.last().key
        while (stack.last().key <= key) stack.removeLast()
      }
    }
    return stack.last()
  }

  override fun nextIndex(): Int {
    check(hasNext())
    return index
  }

  override fun previous(): Map.Entry<K, V> {
    check(hasPrevious())
    when {
      position == Right -> position = Left
      stack.last().left != null -> {
        stack.add(checkNotNull(stack.last().left))
        while (stack.last().right != null) {
          stack.add(checkNotNull(stack.last().right))
        }
      }
      else -> {
        val key = stack.last().key
        while (stack.last().key >= key) stack.removeLast()
      }
    }
    return stack.last()
  }

  override fun previousIndex(): Int {
    check(hasPrevious())
    return index - 1
  }
}
