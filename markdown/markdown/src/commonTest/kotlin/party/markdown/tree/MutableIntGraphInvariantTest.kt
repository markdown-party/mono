package party.markdown.tree

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MutableIntGraphInvariantTest {

  @Test
  fun line_preserves() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    graph.createEdge(a, b)
    assertFalse(graph.moveWouldBreakTreeInvariant(b, a))
  }

  @Test
  fun loop_notPreserve() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    graph.createEdge(a, b)
    assertTrue(graph.moveWouldBreakTreeInvariant(a, b))
  }

  @Test
  fun v_toLine_preserves() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    val c = graph.createVertex()
    graph.createEdge(a, b)
    graph.createEdge(a, c)
    assertFalse(graph.moveWouldBreakTreeInvariant(c, b))
    assertFalse(graph.moveWouldBreakTreeInvariant(b, c))
  }

  @Test
  fun v_toCycle_notPreserve() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    val c = graph.createVertex()
    graph.createEdge(a, b)
    graph.createEdge(a, c)
    assertTrue(graph.moveWouldBreakTreeInvariant(a, b))
    assertTrue(graph.moveWouldBreakTreeInvariant(a, c))
  }

  @Test
  fun line3() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    val c = graph.createVertex()
    graph.createEdge(a, b)
    graph.createEdge(b, c)
    assertTrue(graph.moveWouldBreakTreeInvariant(a, b))
    assertTrue(graph.moveWouldBreakTreeInvariant(a, c))
    assertTrue(graph.moveWouldBreakTreeInvariant(b, c))
    assertFalse(graph.moveWouldBreakTreeInvariant(b, a))
    assertFalse(graph.moveWouldBreakTreeInvariant(c, b))
    assertFalse(graph.moveWouldBreakTreeInvariant(c, a))
  }
}
