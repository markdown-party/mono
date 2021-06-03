package io.github.alexandrepiveteau.echo.core.internal

import io.github.alexandrepiveteau.echo.core.Performance
import kotlin.test.Test
import kotlin.time.measureTime

class ByteGapBufferPerfTest {

  /**
   * A workflow that mutableListOf should optimized for, and where both data structures are designed
   * to feature amortized constant-time insertions.
   */
  @Performance
  @Test
  fun testInsertionPerformance() {
    val buffer = ByteGapBuffer()
    val reference = mutableListOf<Byte>()
    val count = 10_000_000

    val bufferTime = measureTime { repeat(count) { buffer.push(0) } }
    println("Buffer took ${bufferTime.inWholeMilliseconds} ms.")

    val referenceTime = measureTime { repeat(count) { reference.add(0) } }
    println("Reference took ${referenceTime.inWholeMilliseconds} ms.")
  }

  /**
   * A workflow that ByteGapBuffer should be optimized for, with constant-time insertions, and the
   * worst possible case for mutableListOf, with linear-time insertions.
   */
  @Performance
  @Test
  fun testStartInsertionPerformance() {
    val buffer = ByteGapBuffer()
    val reference = mutableListOf<Byte>()
    val count = 500_000

    val bufferTime = measureTime { repeat(count) { buffer.push(0, 0) } }
    println("Buffer took ${bufferTime.inWholeMilliseconds} ms.")

    val referenceTime = measureTime { repeat(count) { reference.add(0, 0) } }
    println("Reference took ${referenceTime.inWholeMilliseconds} ms.")
  }

  /**
   * A workflow that mutableListOf should optimized for, and where both data structures are designed
   * to feature amortized constant-time insertions.
   */
  @Performance
  @Test
  fun testInsertionMemory() {
    // When run with standard settings, this workflow fails for the reference implementation, but
    // succeeds with the ByteGapBuffer, which has a lower overhead because it uses a native backing
    // ByteArray, taking considerably less space.
    val buffer = ByteGapBuffer()
    val reference = mutableListOf<Byte>()
    val count = 250_000_000

    val bufferTime = measureTime { repeat(count) { buffer.push(0) } }
    println("Buffer took ${bufferTime.inWholeMilliseconds} ms.")

    val referenceTime = measureTime { repeat(count) { reference.add(0) } }
    println("Reference took ${referenceTime.inWholeMilliseconds} ms.")
  }
}
