package io.github.alexandrepiveteau.echo.core.internal

import io.github.alexandrepiveteau.echo.core.internal.buffer.MutableByteGapBuffer
import io.github.alexandrepiveteau.echo.core.internal.buffer.mutableByteGapBufferOf
import io.github.alexandrepiveteau.echo.core.internal.buffer.toByteArray
import io.github.alexandrepiveteau.echo.core.internal.buffer.toTypedArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class MutableByteGapBufferToArrayTest {

  @Test
  fun toTypedArray() {
    with(MutableByteGapBuffer(16) { it.toByte() }) {
      assertContentEquals(toTypedArray(), toByteArray().toTypedArray())
    }
  }

  @Test
  fun emptyBuffer() {
    with(mutableByteGapBufferOf()) { assertContentEquals(byteArrayOf(), toByteArray()) }
  }

  @Test
  fun gapAtEnd() {
    with(MutableByteGapBuffer(5) { it.toByte() }) {
      assertContentEquals(ByteArray(5) { it.toByte() }, toByteArray())
    }
  }

  @Test
  fun gapInMiddle() {
    with(MutableByteGapBuffer(5) { it.toByte() }) {
      gap.shift(-3)
      assertContentEquals(ByteArray(5) { it.toByte() }, toByteArray())

      // The gap is preserved.
      assertEquals(2, gap.startIndex)
    }
  }
}
