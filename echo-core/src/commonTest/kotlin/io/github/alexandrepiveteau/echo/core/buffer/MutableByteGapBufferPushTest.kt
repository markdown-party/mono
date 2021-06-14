package io.github.alexandrepiveteau.echo.core.buffer

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

  @Test
  fun pushMulti_atEnd() {
    with(mutableByteGapBufferOf()) {
      push(byteArrayOf(1, 2, 3))
      assertContentEquals(byteArrayOf(1, 2, 3), toByteArray())
    }
  }

  @Test
  fun push_untilEnd() {
    with(mutableByteGapBufferOf()) {
      push(ByteArray(Gap.DefaultSize))
      assertEquals(Gap.DefaultSize, gap.startIndex)
      assertEquals(Gap.DefaultSize, gap.endIndex)
    }
  }

  @Test
  fun pushMulti_managesGapProperty() {
    with(MutableByteGapBuffer(31) { 0 }) {
      push(byteArrayOf(1, 1))
      assertContentEquals(ByteArray(33) { if (it >= 31) 1 else 0 }, toByteArray())
    }
  }

  @Test
  fun pushMulti_atOffset() {
    with(MutableByteGapBuffer(32) { 0 }) {
      push(byteArrayOf(1), offset = 1)
      assertContentEquals(ByteArray(33) { if (it == 1) 1 else 0 }, toByteArray())
    }
  }

  @Test
  fun pushMulti_emptyArray() {
    with(mutableByteGapBufferOf()) {
      push(byteArrayOf(), 0, 0, 0)
      assertEquals(0, size)
      push(byteArrayOf(1, 2, 3), 0, 2, 2)
      assertEquals(0, size)
    }
  }

  @Test
  fun pushMulti_outOfBounds() {
    with(mutableByteGapBufferOf()) {
      assertFails { push(byteArrayOf(), offset = -1) }
      assertFails { push(byteArrayOf(), offset = 1) }
      assertFails { push(byteArrayOf(), 0, 1, 1) }
      assertFails { push(byteArrayOf(1, 2, 3), 0, 1, 0) }
      assertFails { push(byteArrayOf(1, 2, 3), 0, 2, 4) }
    }
  }
}
