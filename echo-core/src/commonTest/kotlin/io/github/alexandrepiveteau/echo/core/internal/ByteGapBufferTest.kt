package io.github.alexandrepiveteau.echo.core.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ByteGapBufferTest {

  @Test
  fun emptyBuffer_isEmpty(): Unit =
      with(ByteGapBuffer()) {
        assertEquals(0, size)
        assertFails { get(0) }
        assertFails { get(Int.MIN_VALUE) }
        assertFails { get(Int.MAX_VALUE) }
        assertFails { push(0, index = 1) }
        assertFails { remove(0) }
        assertFails { remove(Int.MIN_VALUE) }
        assertFails { remove(Int.MAX_VALUE) }
      }

  @Test
  fun emptyBuffer_oneInsertion_oneRemoval(): Unit =
      with(ByteGapBuffer()) {
        push(4)
        assertEquals(1, size)
        assertEquals(4, get(0))
        assertFails { get(1) }
        assertEquals(4, remove(0))
        assertEquals(0, size)
      }

  @Test
  fun emptyBuffer_threeInsertions_notInOrder(): Unit =
      with(ByteGapBuffer()) {
        push(1, 0)
        push(2, 0)
        push(3)
        assertEquals(2, get(0))
        assertEquals(1, get(1))
        assertEquals(3, get(2))
      }

  @Test
  fun emptyBuffer_FourInsertions_array(): Unit =
      with(ByteGapBuffer()) {
        push(4)
        push(byteArrayOf(2, 1, 3), index = 0)
        assertEquals(4, get(3))
        assertEquals(2, get(0))
        assertEquals(1, get(1))
        assertEquals(3, get(2))
        assertEquals(4, size)
      }
}
