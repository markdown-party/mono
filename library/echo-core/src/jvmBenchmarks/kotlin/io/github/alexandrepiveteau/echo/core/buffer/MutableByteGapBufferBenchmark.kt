package io.github.alexandrepiveteau.echo.core.buffer

import kotlinx.benchmark.*

@State(Scope.Thread)
class MutableByteGapBufferBenchmark {

  @Param("1000", "10000", "1000000") var iterations: Int = 0

  private val buffer = mutableByteGapBufferOf()

  @Benchmark
  fun consecutiveInsertsAtEndOfBuffer() {
    buffer.clear()
    repeat(iterations) { buffer.push(it.toByte()) }
  }

  @Benchmark
  fun consecutiveInsertsAtStartOfBuffer() {
    buffer.clear()
    repeat(iterations) { buffer.push(0, offset = 0) }
  }

  private val list = mutableListOf<Byte>()

  @Benchmark
  fun consecutiveInsertsAtEndOfList() {
    list.clear()
    repeat(iterations) { list.add(it.toByte()) }
  }

  @Benchmark
  fun consecutiveInsertsAtStartOfList() {
    list.clear()
    repeat(iterations) { list.add(index = 0, element = 0) }
  }
}
