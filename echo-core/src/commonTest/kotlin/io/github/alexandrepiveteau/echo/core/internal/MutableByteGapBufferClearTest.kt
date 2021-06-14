package io.github.alexandrepiveteau.echo.core.internal

import io.github.alexandrepiveteau.echo.core.internal.buffer.MutableByteGapBuffer
import io.github.alexandrepiveteau.echo.core.internal.buffer.mutableByteGapBufferOf
import kotlin.test.Test
import kotlin.test.assertEquals

class MutableByteGapBufferClearTest {

  @Test
  fun emptyBuffer() {
    with(mutableByteGapBufferOf()) {
      clear()
      assertEquals(0, size)
    }
  }

  @Test
  fun nonEmptyBuffer() {
    with(MutableByteGapBuffer(10) { 0 }) {
      clear()
      assertEquals(0, size)
    }
  }
}
