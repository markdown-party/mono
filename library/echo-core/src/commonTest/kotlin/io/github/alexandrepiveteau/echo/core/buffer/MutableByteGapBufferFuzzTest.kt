package io.github.alexandrepiveteau.echo.core.buffer

import io.github.alexandrepiveteau.echo.core.Performance
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.measureTime

class MutableByteGapBufferFuzzTest {

  /**
   * A sealed class representing the different [Operation] which are supported on a
   * [MutableByteGapBuffer]. These operations also offer an equivalent operation on [MutableList],
   * to compare behaviors.
   */
  sealed interface Operation<out R> {

    /** Performs the operation on a [MutableByteGapBuffer]. */
    fun perform(buffer: MutableByteGapBuffer): R

    /** Performs the operation on a [MutableList]. */
    fun perform(list: MutableList<Byte>): R
  }

  /** Pushes a new [value] at the given [index]. */
  private class Push(val index: Int, val value: Byte) : Operation<Unit> {
    override fun perform(buffer: MutableByteGapBuffer) = buffer.push(value, offset = index)
    override fun perform(list: MutableList<Byte>) = list.add(index = index, value)
  }

  private class PushRange(val index: Int, val bytes: ByteArray) : Operation<Unit> {
    override fun perform(buffer: MutableByteGapBuffer) = buffer.push(bytes, index)
    override fun perform(list: MutableList<Byte>) {
      list.addAll(index, bytes.asList())
    }

    override fun toString(): String {
      return "PushRange(index=$index, bytes=${bytes.contentToString()})"
    }
  }

  /** Removes the value at the given [index]. */
  private class Remove(val index: Int) : Operation<Unit> {
    override fun perform(buffer: MutableByteGapBuffer) = buffer.remove(index, size = 1)
    override fun perform(list: MutableList<Byte>) {
      list.removeAt(index)
    }
  }

  private class RemoveRange(val index: Int, val size: Int) : Operation<Unit> {
    override fun perform(buffer: MutableByteGapBuffer) {
      buffer.remove(index, size = size)
    }
    override fun perform(list: MutableList<Byte>) = repeat(size) { list.removeAt(index) }
  }

  /** Selects a random [Operation] that can be applied for the given [size]. */
  private inline fun Random.nextOp(size: () -> Int): Operation<*> {
    val s = size()
    val prob = nextDouble()
    return if (s > 0 && prob < 0.15) {
      val index = nextInt(s)
      Remove(index)
    } else if (s > 0 && prob < 0.30) {
      val index = nextInt(s)
      val length = nextInt(index, s) - index
      RemoveRange(index, length)
    } else if (prob < 0.65) {
      val index = nextInt(s + 1)
      val byte = nextInt().toByte()
      Push(index, byte)
    } else {
      val index = nextInt(s + 1)
      val bytes = nextBytes(nextInt(5))
      PushRange(index, bytes)
    }
  }

  @Test
  fun test_fuzzCompare() {
    val buffer = mutableByteGapBufferOf()
    val reference = mutableListOf<Byte>()
    val random = Random(0xDEADBEEF)

    repeat(10_000) {
      val op = random.nextOp(buffer::size)
      op.perform(reference)
      op.perform(buffer)
      assertEquals(reference.size, buffer.size)
      assertContentEquals(reference, buffer.toByteArray().asList())
    }

    assertEquals(reference.size, buffer.size)
    assertContentEquals(reference, buffer.toByteArray().asList())
  }

  @Performance
  @Test
  fun test_randomInsertionsPerformance() {
    val count = 100_000

    val buffer = mutableByteGapBufferOf()
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
