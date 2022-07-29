package io.github.alexandrepiveteau.echo.core.log.tree

import kotlin.math.max

internal class AVLTree<T : Comparable<T>> {

  /**
   * A node within an [AVLTree], which may contain some values of type [T].
   *
   * @param value the value of the node.
   * @param parent the parent of this [AVLNode], if it exists.
   * @param left the left child of this node, if it exists.
   * @param leftHeight the height of the left subtree.
   * @param right the right child of this node, if it exists.
   * @param rightHeight the height of the right subtree.
   */
  internal data class AVLNode<T>(
      val value: T,
      var parent: AVLNode<T>?,
      var height: Int,
      var left: AVLNode<T>?,
      var right: AVLNode<T>?,
  ) {

    /** The balance of an [AVLNode]. */
    val balance: Int
      get() = (right?.height ?: 0) - (left?.height ?: 0)

    /**
     * A helper function which recomputes the height of the current [AVLNode] using the [right] and
     * [left] subtrees.
     */
    private fun updateHeight() {
      height = max(left?.height ?: 0, right?.height ?: 0) + 1
    }

    /**
     * Rotates the [AVLNode] to the left. This corresponds to the following transformation if
     * [rotateLeft] is applied to `A` :
     *
     * ```
     *     A              B
     *    / \            / \
     *   a   B    =>    A   c
     *      / \        / \
     *     b   c      a   b
     * ```
     *
     * @return the new root of the subtree.
     */
    fun rotateLeft(): AVLNode<T> {
      val pivot = checkNotNull(right) { "Can't rotate left when no right child." }
      pivot.parent = parent
      parent = pivot
      right = pivot.left
      pivot.left = this
      updateHeight()
      pivot.updateHeight()
      return pivot
    }

    /**
     * Rotates the [AVLNode] to the right. This corresponds to the following transformation if
     * [rotateRight] is applied to `B` :
     *
     * ```
     *     B            A
     *    / \          / \
     *   A   c  =>    a   B
     *  / \              / \
     * a   b            b   c
     * ```
     *
     * @return the new root of the subtree.
     */
    fun rotateRight(): AVLNode<T> {
      val pivot = checkNotNull(left) { "Can't rotate right when no left child." }
      pivot.parent = parent
      parent = pivot
      left = pivot.right
      pivot.right = this
      updateHeight()
      pivot.updateHeight()
      return pivot
    }

    /*
    /** Returns the next [AVLNode], if it exists. */
    fun next(): AVLNode<T>? = nextRight() ?: nextParent()

    private fun nextRight(): AVLNode<T>? {
      var current = right ?: return null
      var left = current.left
      while (left != null) {
        current = left
        left = current.left
      }
      return current
    }

    private fun nextParent(): AVLNode<T>? {
      TODO()
    }
     */
  }

  /** The root off the tree. Might be null if no elements have been inserted yet. */
  private var root: AVLNode<T>? = null

  /**
   * Returns true iff the given [value] is contained within the [AVLTree], in O(log(n)).
   *
   * @param value the value whose presence is checked.
   * @return true iff the value is present in the tree.
   */
  operator fun contains(value: T): Boolean {
    var current = root
    while (current != null) {
      current =
          when {
            value > current.value -> current.left
            value < current.value -> current.right
            else -> return true
          }
    }
    return false
  }

  /**
   * Inserts the given [value] in the [AVLTree], in O(log(n)).
   *
   * @param value the item which is inserted.
   */
  fun insert(value: T) {
    root = add(root, value)
  }

  private fun add(node: AVLNode<T>?, value: T): AVLNode<T> {
    // TODO : Handle setting the parent.
    if (node == null) return AVLNode(value, null, 1, null, null)
    when {
      value < node.value -> {
        val left = add(node.left, value)
        left.parent = node
        node.left = left
      }
      value > node.value -> {
        val right = add(node.right, value)
        right.parent = node
        node.right = right
      }
      else -> return node // Skip the insertion on duplicate entries.
    }
    return node // TODO : Balance the nodes.
  }
}
