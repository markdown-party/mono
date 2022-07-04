package io.github.alexandrepiveteau.echo.core.buffer

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class MutableByteGapBufferBuilderTest {

  @Test
  fun emptyBuffer() {
    with(mutableByteGapBufferOf()) {
      assertEquals(0, size)
      assertContentEquals(byteArrayOf(), toByteArray())
    }
    with(MutableByteGapBuffer(0) { 0 }) {
      assertEquals(0, size)
      assertContentEquals(byteArrayOf(), toByteArray())
    }
  }

  @Test
  fun nonEmptyBuffer_fails() {
    assertFails { MutableByteGapBuffer(-1) { 0 } }
  }

  @Test
  fun nonEmptyBuffer_succeeds() {
    with(MutableByteGapBuffer(16) { it.toByte() }) {
      assertEquals(16, size)
      assertContentEquals(ByteArray(16) { it.toByte() }, toByteArray())
    }
  }
}
