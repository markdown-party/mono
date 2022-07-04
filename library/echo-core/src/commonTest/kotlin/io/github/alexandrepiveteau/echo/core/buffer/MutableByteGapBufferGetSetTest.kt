package io.github.alexandrepiveteau.echo.core.buffer

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class MutableByteGapBufferGetSetTest {

  @Test
  fun empty_outOfBounds() {
    with(mutableByteGapBufferOf()) {
      assertFails { get(-1) }
      assertFails { get(0) }
      assertFails { set(-1, 0) }
      assertFails { set(0, 0) }
    }
  }

  @Test
  fun beforeGap() {
    with(MutableByteGapBuffer(5) { it.toByte() }) {
      assertEquals(5, gap.startIndex)
      for (i in 0 until 5) {
        assertEquals(i.toByte(), get(i))
        set(i, (get(i) * 2).toByte())
      }
      assertContentEquals(ByteArray(5) { (it * 2).toByte() }, toByteArray())
    }
  }

  @Test
  fun afterGap() {
    with(MutableByteGapBuffer(5) { it.toByte() }) {
      gap.shift(-5)
      assertEquals(0, gap.startIndex)
      for (i in 0 until 5) {
        assertEquals(i.toByte(), get(i))
        set(i, (get(i) * 2).toByte())
      }
      assertContentEquals(ByteArray(5) { (it * 2).toByte() }, toByteArray())
    }
  }
}
