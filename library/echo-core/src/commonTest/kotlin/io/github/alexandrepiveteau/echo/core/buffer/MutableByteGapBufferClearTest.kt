package io.github.alexandrepiveteau.echo.core.buffer

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
