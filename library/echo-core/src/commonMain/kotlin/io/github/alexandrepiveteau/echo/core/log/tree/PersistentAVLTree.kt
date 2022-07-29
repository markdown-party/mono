package io.github.alexandrepiveteau.echo.core.log.tree

import io.github.alexandrepiveteau.echo.core.log.tree.PersistentAVLTree.AVLNode

/**
 * A persistent AVL Tree data structure.
 *
 * @param T the type of the elements in the [PersistentAVLTree].
 * @property root the root [AVLNode]. Might be null if the tree is empty.
 */
internal class PersistentAVLTree<T : Comparable<T>>
private constructor(
    val root: AVLNode<T>?,
) {

  /**
   * A node within an [PersistentAVLTree], which may contain some values of type [T].
   *
   * @param value the value of the node.
   * @param height the height of the tree. At least 1.
   * @param left the left child of this node, if it exists.
   * @param right the right child of this node, if it exists.
   */
  internal data class AVLNode<T>(
      val value: T,
      val height: Int,
      val left: AVLNode<T>?,
      val right: AVLNode<T>?,
  ) {

    /** The balance factor of an [AVLNode]. Must be in the range [-2, 2]. */
    private val balance: Int
      get() = (right?.height ?: 0) - (left?.height ?: 0)

    /**
     * Balances the [AVLNode], by applying some rotations, and returns a new balanced node with a
     * [balance] in the range [-1, 1].
     */
    fun balance(): AVLNode<T> =
        when (this.balance) {
          -2 ->
              when (checkNotNull(left).balance) {
                1 -> rotateLeftRight()
                else -> rotateRight()
              }
          2 ->
              when (checkNotNull(right).balance) {
                -1 -> rotateRightLeft()
                else -> rotateLeft()
              }
          else -> this // The node is already balanced.
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
    private fun rotateLeft(): AVLNode<T> {
      val pivot = checkNotNull(right) { "Can't rotate left when no right child." }
      val newLeft =
          this.copy(
              height = maxHeight(this.left, pivot.left) + 1,
              right = pivot.left,
          )
      return pivot.copy(
          height = maxHeight(newLeft, pivot.right) + 1,
          left = newLeft,
      )
    }

    /**
     * Rotates the [AVLNode] to the right. This corresponds to the following transformation if
     * [rotateRight] is applied to `B` :
     *
     * ```
     *       B          A
     *      / \        / \
     *     A   c  =>  a   B
     *    / \            / \
     *   a   b          b   c
     * ```
     *
     * @return the new root of the subtree.
     */
    private fun rotateRight(): AVLNode<T> {
      val pivot = checkNotNull(left) { "Can't rotate right when no left child." }
      val newRight =
          this.copy(
              height = maxHeight(this.right, pivot.right) + 1,
              left = pivot.right,
          )
      return pivot.copy(
          height = maxHeight(newRight, pivot.left) + 1,
          right = newRight,
      )
    }

    /**
     * Rotates the [AVLNode] to the left, and then to the right. This corresponds to the following
     * transformations if [rotateLeftRight] is applied to `A` :
     *
     * ```
     *       A              A            C
     *      / \            / \          / \
     *     B   d          C   d        B   A
     *    / \     =>     / \     =>   /|   |\
     *   a  C           B   c        a b   c d
     *     / \         / \
     *    b   c       a   b
     * ```
     */
    private fun rotateLeftRight(): AVLNode<T> {
      val left = checkNotNull(left) { "Can't rotate left-right when no left child." }
      val rotated = left.rotateLeft()
      return copy(
              height = maxHeight(rotated, right) + 1,
              left = rotated,
          )
          .rotateRight()
    }

    /**
     * Rotates the [AVLNode] to the right, and then to the left. This corresponds to the following
     * transformations if [rotateRightLeft] is applied to `A` :
     *
     * ```
     *     A            A                C
     *    / \          / \              / \
     *   a   B        a   C            A   B
     *      / \   =>     / \     =>   /|   |\
     *     C   d        b   B        a b   c d
     *    / \              / \
     *   b   c            c   d
     * ```
     */
    private fun rotateRightLeft(): AVLNode<T> {
      val right = checkNotNull(right) { "Can't rotate right-left when no right child." }
      val rotated = right.rotateRight()
      return copy(
              height = maxHeight(left, rotated) + 1,
              right = rotated,
          )
          .rotateLeft()
    }
  }

  /** Creates an empty [PersistentAVLTree]. */
  constructor() : this(null)

  /**
   * Returns true iff the given [value] is contained within the [PersistentAVLTree], in O(log(n)).
   *
   * @param value the value whose presence is checked.
   * @return true iff the value is present in the tree.
   */
  operator fun contains(value: T): Boolean {
    var current = root
    while (current != null) {
      current =
          when {
            value > current.value -> current.right
            value < current.value -> current.left
            else -> return true
          }
    }
    return false
  }

  /**
   * Inserts the given [value] in the [PersistentAVLTree], in O(log(n)).
   *
   * @param value the item which is inserted.
   */
  operator fun plus(value: T): PersistentAVLTree<T> {
    return PersistentAVLTree(add(root, value))
  }

  /**
   * Inserts the given [value] in the provided [root], and returns the updated [AVLNode].
   *
   * @param root the [AVLNode] in which the value is inserted.
   * @param value the inserted value.
   * @return the updated root [AVLNode].
   */
  private fun add(root: AVLNode<T>?, value: T): AVLNode<T> {
    return when {
      root == null -> AVLNode(value, 1, null, null)
      value < root.value -> {
        val left = add(root.left, value)
        root
            .copy(
                height = maxHeight(left, root.right) + 1,
                left = left,
            )
            .balance()
      }
      value > root.value -> {
        val right = add(root.right, value)
        root
            .copy(
                height = maxHeight(right, root.left) + 1,
                right = right,
            )
            .balance()
      }
      else -> root // Skip the insertion on duplicate entries.
    }
  }

  // TODO : Removal of items.

  override fun toString(): String = "PersistentAVLTree(${root.toString()})"
}

/**
 * Returns the maximum height of a set of [AVLNode].
 *
 * @param nodes the set of [AVLNode] for which the maximum height is computed.
 */
private fun maxHeight(vararg nodes: AVLNode<*>?): Int {
  return nodes.maxOfOrNull { it?.height ?: 0 } ?: 0
}
