package io.github.alexandrepiveteau.echo.core.internal

import io.github.alexandrepiveteau.echo.core.Performance
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

class ByteGapBufferFuzzTest {

  /**
   * A sealed class representing the different [Operation] which are supported on a [ByteGapBuffer].
   * These operations also offer an equivalent operation on [MutableList], to compare behaviors.
   */
  private sealed interface Operation<out R> {

    /** Performs the operation on a [ByteGapBuffer]. */
    fun perform(buffer: ByteGapBuffer): R

    /** Performs the operation on a [MutableList]. */
    fun perform(list: MutableList<Byte>): R
  }

  /** Pushes a new [value] at the given [index]. */
  private data class Push(val index: Int, val value: Byte) : Operation<Unit> {
    override fun perform(buffer: ByteGapBuffer) = buffer.push(value, index = index)
    override fun perform(list: MutableList<Byte>) = list.add(index = index, value)
  }

  /** Removes the value at the given [index]. */
  private data class Remove(val index: Int) : Operation<Byte> {
    override fun perform(buffer: ByteGapBuffer) = buffer.remove(index)
    override fun perform(list: MutableList<Byte>) = list.removeAt(index)
  }

  /** Selects a random [Operation] that can be applied for the given size. */
  private inline fun Random.nextOp(size: () -> Int): Operation<*> {
    val s = size()
    return if (s > 0 && nextDouble() < 0.3) {
      val index = nextInt(s)
      Remove(index)
    } else {
      val index = nextInt(s + 1)
      val byte = nextInt().toByte()
      Push(index, byte)
    }
  }

  @Test
  fun test_fuzzCompare() {
    val buffer = ByteGapBuffer()
    val reference = mutableListOf<Byte>()
    val random = Random(0xDEADBEEF)

    repeat(10_000) {
      val op = random.nextOp(buffer::size)
      assertEquals(op.perform(buffer), op.perform(reference))
    }

    assertEquals(reference.size, buffer.size)
    for (index in reference.indices) {
      assertEquals(reference[index], buffer[index])
    }
  }

  @Performance
  @Test
  fun test_randomInsertionsPerformance() {
    val count = 100_000

    val buffer = ByteGapBuffer()
    val bufferRandom = Random(0xDEADBEEF)

    val reference = mutableListOf<Byte>()
    val referenceRandom = Random(0xDEADBEEF)

    val bufferTime = measureTime {
      repeat(count) { bufferRandom.nextOp(buffer::size).perform(buffer) }
    }
    val referenceTime = measureTime {
      repeat(count) { referenceRandom.nextOp(reference::size).perform(reference) }
    }

    println("Buffer took ${bufferTime.inWholeMilliseconds} ms.")
    println("Reference took ${referenceTime.inWholeMilliseconds} ms.")
  }
}
