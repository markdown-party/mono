package io.github.alexandrepiveteau.echo.core.internal

import io.github.alexandrepiveteau.echo.core.internal.buffer.Gap
import io.github.alexandrepiveteau.echo.core.internal.buffer.MutableByteGapBuffer
import io.github.alexandrepiveteau.echo.core.internal.buffer.mutableByteGapBufferOf
import kotlin.test.Test
import kotlin.test.assertEquals

class MutableByteGapBufferGapTest {

  @Test
  fun emptyBuffer() {
    with(mutableByteGapBufferOf()) {
      assertEquals(0, gap.startIndex)
      assertEquals(io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize, gap.endIndex)
    }
  }

  @Test
  fun emptyBuffer_shift() {
    with(mutableByteGapBufferOf()) {
      gap.shift(1)
      assertEquals(0, gap.startIndex)
      assertEquals(io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize, gap.endIndex)
      gap.shift(-1)
      assertEquals(0, gap.startIndex)
      assertEquals(io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize, gap.endIndex)
    }
  }

  @Test
  fun initialGap_atEnd() {
    with(MutableByteGapBuffer(10) { 0 }) {
      assertEquals(10, gap.startIndex)
      assertEquals(io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize, gap.endIndex)
    }
  }

  @Test
  fun gap_canShift() {
    with(MutableByteGapBuffer(10) { 0 }) {
      gap.shift(-4)
      assertEquals(6, gap.startIndex)
      assertEquals(io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize - 4, gap.endIndex)
      gap.shift(2)
      assertEquals(8, gap.startIndex)
      assertEquals(io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize - 2, gap.endIndex)
    }
  }

  @Test
  fun initialBuffer_notResized() {
    with(MutableByteGapBuffer(Gap.DefaultSize) { 0 }) {
      assertEquals(io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize, gap.startIndex)
      assertEquals(io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize, gap.endIndex)
      gap.shift(-1)
      assertEquals(io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize - 1, gap.startIndex)
      assertEquals(io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize - 1, gap.endIndex)
    }
  }

  @Test
  fun initialBuffer_resized() {
    with(MutableByteGapBuffer(2 * Gap.DefaultSize + 1) { 0 }) {
      assertEquals(2 * io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize - 1, gap.endIndex - gap.startIndex)
      assertEquals(2 * io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize + 1, size)
    }
  }
}
