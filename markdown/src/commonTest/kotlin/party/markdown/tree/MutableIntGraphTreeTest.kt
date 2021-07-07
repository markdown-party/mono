package party.markdown.tree

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MutableIntGraphTreeTest {

  @Test
  fun empty_notTree() {
    val graph = MutableIntGraph()
    assertFalse(graph.isTree())
  }

  @Test
  fun single_tree() {
    val graph = MutableIntGraph()
    graph.createVertex()
    assertTrue(graph.isTree())
  }

  @Test
  fun singleLoop_notTree() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    graph.createEdge(a, a)
    assertFalse(graph.isTree())
  }

  @Test
  fun disconnected_notTree() {
    val graph = MutableIntGraph()
    graph.createVertex()
    graph.createVertex()
    assertFalse(graph.isTree())
  }

  @Test
  fun line_tree() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    graph.createEdge(a, b)
    assertTrue(graph.isTree())
  }

  @Test
  fun loop_notTree() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    graph.createEdge(a, b)
    graph.createEdge(b, a)
    assertFalse(graph.isTree())
  }

  @Test
  fun loop3_notTree() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    val c = graph.createVertex()
    graph.createEdge(a, b)
    graph.createEdge(b, c)
    graph.createEdge(c, a)
    assertFalse(graph.isTree())
  }

  @Test
  fun v_tree() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    val c = graph.createVertex()
    graph.createEdge(b, a)
    graph.createEdge(b, c)
    assertTrue(graph.isTree())
  }

  @Test
  fun star2_tree() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    val c = graph.createVertex()
    graph.createEdge(b, a)
    graph.createEdge(b, c)
    assertTrue(graph.isTree())
  }

  @Test
  fun star100_tree() {
    val graph = MutableIntGraph()
    val center = graph.createVertex()
    for (i in 1..100) {
      val e = graph.createVertex()
      graph.createEdge(center, e)
    }
    assertTrue(graph.isTree())
  }

  @Test
  fun star100inv_notTree() {
    val graph = MutableIntGraph()
    val center = graph.createVertex()
    for (i in 1..100) {
      val e = graph.createVertex()
      graph.createEdge(e, center)
    }
    assertFalse(graph.isTree())
  }

  @Test
  fun dag_notTree() {
    val graph = MutableIntGraph()
    val a = graph.createVertex()
    val b = graph.createVertex()
    val c = graph.createVertex()
    graph.createEdge(a, b)
    graph.createEdge(b, c)
    graph.createEdge(a, c)
    assertFalse(graph.isTree())
  }
}
