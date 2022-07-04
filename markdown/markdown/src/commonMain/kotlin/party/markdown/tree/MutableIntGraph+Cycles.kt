package party.markdown.tree

private const val NoRoot = -1

/** Finds the root vertex of this graph, that is a vertex with no parent. */
private fun MutableIntGraph.findRoot(): Int {

  // Store the number of parents that reference each vertex.
  val parents = IntArray(size)
  for (i in 0 until size) {
    neighboursForEach(i) { parents[it]++ }
  }

  // Search for a unique vertex [v] for which parents[v] == 0.
  var root = NoRoot
  for (i in 0 until size) {
    if (parents[i] == 0 && root != NoRoot) return NoRoot
    if (parents[i] == 0) root = i
  }

  return root
}

/**
 * Performs an iterative DFS traversal, starting at the [root]. Visited nodes are marked in the
 * [visited] array.
 *
 * If a node is visited twice, this function returns false. It indicates that we are not in a tree.
 *
 * @param root the origin of the traversal.
 * @param visited a vector indicating whether a given node was traversed or not.
 */
private fun MutableIntGraph.treeDfs(root: Int, visited: BooleanArray): Boolean {
  val queue = ArrayDeque<Int>().apply { add(root) }
  while (queue.isNotEmpty()) {
    val head = queue.removeFirst()
    if (visited[head]) return false // we may have a DAG, but it's definitely not a tree.
    visited[head] = true
    neighboursForEach(head) { n -> queue.addFirst(n) }
  }
  return true
}

/** Returns true iff all the values in this `BooleanArray` are true. */
private fun BooleanArray.allTrue(): Boolean {
  for (b in this) if (!b) return false
  return true
}

/**
 * An iterative implementation that will check whether the given graph is a tree. The following
 * steps are performed :
 *
 * 1. The root of the tree is found. If it does not exist, return false.
 * 2. A DFS is performed to check that each node has exactly one parent. If not, return false.
 * 3. We check if all the nodes were traversed during the DFS. If not, return false.
 *
 * @return `true` iff this directed graph is a tree.
 */
internal fun MutableIntGraph.isTree(): Boolean {

  // Step 1.
  val root = findRoot()
  if (root == NoRoot) return false

  // Step 2.
  val visited = BooleanArray(size)
  val valid = treeDfs(root, visited)
  if (!valid) return false

  // Step 3.
  return visited.allTrue()
}
