package party.markdown.tree

import io.github.alexandrepiveteau.echo.core.buffer.*

internal class MutableIntGraph {

  /** The adjacency list for the [MutableIntGraph]. Each adjacency list is sorted. */
  private val adjacency = mutableGapBufferOf<MutableIntGapBuffer>()

  /** Returns the size of the [MutableIntGraph]. */
  val size: Int
    get() = adjacency.size

  /**
   * Creates a new vertex in the [MutableIntGraph].
   *
   * @return the vertex identifier that was created.
   */
  fun createVertex(): Int {
    val id = adjacency.size
    adjacency.push(mutableIntGapBufferOf())
    return id
  }

  /**
   * Creates an edge [from] a vertex [to] a vertex.
   *
   * @param from the origin vertex.
   * @param to the target vertex.
   * @return true iff a new edge was created. False if [from] or [to] were missing, or the edge
   * already present.
   */
  fun createEdge(from: Int, to: Int): Boolean {
    if (!contains(from) || !contains(to)) return false
    val point = adjacency[from].binarySearch(to)
    if (point >= 0) return false // Found.
    adjacency[from].push(to, -(point + 1)) // Inverted insertion point.
    return true
  }

  /**
   * Removes an edge [from] a vertex [to] a vertex.
   *
   * @param from the origin vertex.
   * @param to the target vertex.
   * @return true iff the edge was present and deleted. False if [from] or [to] were missing, or the
   * edge not present.
   */
  fun removeEdge(from: Int, to: Int): Boolean {
    if (!contains(from) || !contains(to)) return false
    val index = adjacency[from].binarySearch(to)
    if (index < 0) return false // Not found.
    adjacency[from].remove(index)
    return true
  }

  /** Returns true iff a vertex with the given [value] was created. */
  operator fun contains(value: Int): Boolean {
    return value in 0 until adjacency.size
  }

  /**
   * Returns true if the directed edge going [from] a vertex [to] another vertex is present in the
   * graph.
   *
   * @param from the origin vertex.
   * @param to the target vertex.
   */
  fun containsEdge(from: Int, to: Int): Boolean {
    if (!contains(from) || !contains(to)) return false
    val list = adjacency[from]
    return list.binarySearch(to) >= 0
  }

  /**
   * Returns a sorted [IntArray] of the neighbours of the given vertex. Neighbours are considered in
   * a directed fashion; that is, a neighbour y of x has `containsEdge(from=x, to=y) == true`.
   *
   * @param vertex the origin vertex.
   */
  fun neighbours(vertex: Int): IntArray {
    return adjacency[vertex].toIntArray()
  }

  /** Returns the count of neighbours for the given [vertex]. */
  fun neighboursSize(vertex: Int): Int {
    return adjacency[vertex].size
  }

  /**
   * Performs the given action for each neighbour of the given vertex.
   *
   * @param vertex the searched vertex.
   * @param f the action called, with the neighbour index.
   */
  inline fun neighboursForEach(vertex: Int, f: (Int) -> Unit) {
    val list = adjacency[vertex]
    for (i in 0 until list.size) {
      f(list[i])
    }
  }

  /**
   * Removes the given vertex from the [MutableIntGraph]. Only the last added vertex can actually be
   * removed; otherwise, an exception will be thrown.
   *
   * @param vertex the vertex to remove.
   */
  fun removeVertex(vertex: Int) {
    require(vertex == size - 1) { "Can only remove the last vertex." }
    // Remove from all the other vertices first.
    for (v in 0 until size) {
      val index = adjacency[v].binarySearch(vertex)
      if (index >= 0) adjacency[v].remove(index)
    }
    adjacency.remove(size - 1)
  }
}
