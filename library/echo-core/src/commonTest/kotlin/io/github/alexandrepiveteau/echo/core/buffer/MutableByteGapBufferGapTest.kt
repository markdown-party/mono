package io.github.alexandrepiveteau.echo.core.buffer

import kotlin.test.Test
import kotlin.test.assertEquals

class MutableByteGapBufferGapTest {

  @Test
  fun emptyBuffer() {
    with(mutableByteGapBufferOf()) {
      assertEquals(0, gap.startIndex)
      assertEquals(Gap.DefaultSize, gap.endIndex)
    }
  }

  @Test
  fun emptyBuffer_shift() {
    with(mutableByteGapBufferOf()) {
      gap.shift(1)
      assertEquals(0, gap.startIndex)
      assertEquals(Gap.DefaultSize, gap.endIndex)
      gap.shift(-1)
      assertEquals(0, gap.startIndex)
      assertEquals(Gap.DefaultSize, gap.endIndex)
    }
  }

  @Test
  fun initialGap_atEnd() {
    with(MutableByteGapBuffer(10) { 0 }) {
      assertEquals(10, gap.startIndex)
      assertEquals(Gap.DefaultSize, gap.endIndex)
    }
  }

  @Test
  fun gap_canShift() {
    with(MutableByteGapBuffer(10) { 0 }) {
      gap.shift(-4)
      assertEquals(6, gap.startIndex)
      assertEquals(Gap.DefaultSize - 4, gap.endIndex)
      gap.shift(2)
      assertEquals(8, gap.startIndex)
      assertEquals(Gap.DefaultSize - 2, gap.endIndex)
    }
  }

  @Test
  fun initialBuffer_notResized() {
    with(MutableByteGapBuffer(Gap.DefaultSize) { 0 }) {
      assertEquals(Gap.DefaultSize, gap.startIndex)
      assertEquals(Gap.DefaultSize, gap.endIndex)
      gap.shift(-1)
      assertEquals(Gap.DefaultSize - 1, gap.startIndex)
      assertEquals(Gap.DefaultSize - 1, gap.endIndex)
    }
  }

  @Test
  fun initialBuffer_resized() {
    with(MutableByteGapBuffer(2 * Gap.DefaultSize + 1) { 0 }) {
      assertEquals(2 * Gap.DefaultSize - 1, gap.endIndex - gap.startIndex)
      assertEquals(2 * Gap.DefaultSize + 1, size)
    }
  }
}
