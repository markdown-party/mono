package party.markdown.tree

private fun MutableIntGraph.findParent(vertex: Int): Int? {
  for (i in 0 until size) {
    neighboursForEach(i) { v -> if (v == vertex) return i }
  }
  return null
}

/**
 * Returns true iff moving the provided [vertex] such that its parent is the provided [anchor] would
 * break the tree invariants. This means that you should only perform a move if this method returns
 * `true`.
 *
 * If the [MutableIntGraph] wasn't a tree already, `false` will be returned.
 *
 * @param vertex the vertex that will be moved.
 * @param anchor the destination vertex.
 */
internal fun MutableIntGraph.moveWouldBreakTreeInvariant(
    vertex: Int,
    anchor: Int,
): Boolean {
  // First, check if we already satisfy the invariant. Each node will therefore have up to one
  // parent.
  if (!isTree()) return true

  // Check if we already satisfy the requested move. If so, we know that moving will not break the
  // invariants and that we'll keep a tree.
  if (containsEdge(anchor, vertex)) return false

  // Find the current parent.
  val parent = findParent(vertex)

  // Tentatively perform the move, and revert it.
  if (parent != null) removeEdge(parent, vertex)
  createEdge(anchor, vertex)
  val valid = isTree()
  removeEdge(anchor, vertex)
  if (parent != null) createEdge(parent, vertex)

  // Return the condition check result.
  return !valid
}
