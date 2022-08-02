package io.github.alexandrepiveteau.echo.core.log.tree

import kotlinx.benchmark.*

@State(Scope.Thread)
class PersistentAVLTreeBenchmark {

  @Param("1000", "10000", "1000000") var iterations: Int = 0

  @Benchmark
  fun consecutiveInsertionsAtEnd(hole: Blackhole) {
    var tree = PersistentAVLTree<Int, Int>()
    repeat(iterations) { tree = tree.set(it, it) }
    hole.consume(tree)
  }
}
