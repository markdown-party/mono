package io.github.alexandrepiveteau.echo.core.internal

import io.github.alexandrepiveteau.echo.core.internal.buffer.Gap
import io.github.alexandrepiveteau.echo.core.internal.buffer.MutableByteGapBuffer
import io.github.alexandrepiveteau.echo.core.internal.buffer.mutableByteGapBufferOf
import io.github.alexandrepiveteau.echo.core.internal.buffer.toByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class MutableByteGapBufferPushTest {

  @Test
  fun pushSingle_atEnd() {
    with(mutableByteGapBufferOf()) {
      push(1)
      assertEquals(get(0), 1)
      assertContentEquals(byteArrayOf(1), toByteArray())
    }
  }

  @Test
  fun pushSingle_managesGapProperly() {
    with(mutableByteGapBufferOf()) {
      repeat(Gap.DefaultSize) { push(0) }
      assertEquals(Gap.DefaultSize, gap.startIndex)
      assertEquals(Gap.DefaultSize, gap.endIndex)
      push(0)
      assertEquals(Gap.DefaultSize + 1, gap.startIndex)
      assertEquals(2 * Gap.DefaultSize, gap.endIndex)
    }
  }

  @Test
  fun pushSingle_atOffset() {
    with(MutableByteGapBuffer(32) { 0 }) {
      push(1, offset = 1)
      assertContentEquals(ByteArray(33) { if (it == 1) 1 else 0 }, toByteArray())
    }
  }

  @Test
  fun pushSingle_outOfBounds() {
    with(mutableByteGapBufferOf()) {
      assertFails { push(0, offset = -1) }
      assertFails { push(0, offset = 1) }
    }
  }
}
