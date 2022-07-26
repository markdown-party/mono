package io.github.alexandrepiveteau.echo.core.buffer

import kotlinx.benchmark.*

@State(Scope.Thread)
class MutableByteGapBufferBenchmark {

  @Param("1000", "10000", "1000000") var iterations: Int = 0

  private val buffer = mutableByteGapBufferOf()

  @Benchmark
  fun consecutiveInsertsAtEndOfBuffer(hole: Blackhole) {
    buffer.clear()
    repeat(iterations) { buffer.push(it.toByte()) }
    repeat(iterations) { hole.consume(buffer[it]) }
  }

  @Benchmark
  fun consecutiveInsertsAtStartOfBuffer(hole: Blackhole) {
    buffer.clear()
    repeat(iterations) { buffer.push(0, offset = 0) }
    repeat(iterations) { hole.consume(buffer[it]) }
  }

  private val list = mutableListOf<Byte>()

  @Benchmark
  fun consecutiveInsertsAtEndOfList(hole: Blackhole) {
    list.clear()
    repeat(iterations) { list.add(it.toByte()) }
    repeat(iterations) { hole.consume(list[it]) }
  }

  @Benchmark
  fun consecutiveInsertsAtStartOfList(hole: Blackhole) {
    list.clear()
    repeat(iterations) { list.add(index = 0, element = 0) }
    repeat(iterations) { hole.consume(list[it]) }
  }
}
