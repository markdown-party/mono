package io.github.alexandrepiveteau.echo.core.buffer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class MutableByteGapBufferRemoveTest {

  @Test
  fun empty() {
    with(MutableByteGapBuffer(1)) {
      remove(0, 0)
      assertEquals(1, size)
    }
  }

  @Test
  fun simple() {
    with(MutableByteGapBuffer(1)) {
      remove(0)
      assertEquals(0, size)
    }
  }

  @Test
  fun outOfBounds() {
    with(mutableByteGapBufferOf()) { assertFails { remove(1, 0) } }
    with(MutableByteGapBuffer(1)) {
      assertFails { remove(-1) }
      assertFails { remove(1) }
      assertFails { remove(0, size = -1) }
      assertFails { remove(0, size = 2) }
    }
  }

  @Test
  fun aroundGap() {
    with(MutableByteGapBuffer(Gap.DefaultSize - 1) { it.toByte() }) {
      gap.shift(-5)
      remove(0, Gap.DefaultSize - 1)
      assertEquals(0, size)
    }
  }
}
