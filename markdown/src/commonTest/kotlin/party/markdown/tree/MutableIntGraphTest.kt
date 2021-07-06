package party.markdown.tree

import kotlin.test.*

class MutableIntGraphTest {

  @Test
  fun empty() {
    val graph = MutableIntGraph()
    assertEquals(0, graph.size)
    assertFalse(graph.contains(0))
    assertFalse(graph.containsEdge(0, 1))
    assertFails { graph.neighbours(0) }
    assertFails { graph.neighboursSize(0) }
    assertFails { graph.neighboursForEach(0) {} }
  }

  @Test
  fun empty_remove() {
    val graph = MutableIntGraph()
    assertFalse(graph.removeEdge(0, 1))
  }

  @Test
  fun createVertex() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    assertTrue(graph.contains(a))
    assertContentEquals(intArrayOf(), graph.neighbours(a))
    assertEquals(a, graph.neighboursSize(0))
    graph.neighboursForEach(a) { fail() }
  }

  @Test
  fun createAndDeleteEdge() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    graph.createEdge(a, b)
    assertTrue(graph.containsEdge(a, b))
    graph.removeEdge(a, b)
    assertFalse(graph.containsEdge(a, b))
  }

  @Test
  fun createTwiceAndDeleteEdge() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    graph.createEdge(a, b)
    graph.createEdge(a, b)
    graph.removeEdge(a, b)
    assertFalse(graph.containsEdge(a, b))
  }
}
