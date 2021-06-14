package io.github.alexandrepiveteau.echo.core.internal

import io.github.alexandrepiveteau.echo.core.internal.buffer.mutableByteGapBufferOf
import io.github.alexandrepiveteau.echo.core.internal.buffer.toByteArray
import io.github.alexandrepiveteau.echo.core.internal.buffer.toTypedArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class MutableByteGapBufferEmptyTest {

  @Test
  fun emptyBuffer() {
    with(mutableByteGapBufferOf()) {
      assertEquals(0, size)
      assertFails { get(0) }
      assertFails { set(offset = 0, 0) }
      assertEquals(0, gap.startIndex)
      assertEquals(io.github.alexandrepiveteau.echo.core.internal.buffer.Gap.DefaultSize, gap.endIndex)
      assertContentEquals(byteArrayOf(), toByteArray())
      assertContentEquals(emptyArray(), toTypedArray())
    }
  }

  @Test
  fun emptyBuffer_oneInsertion_oneRemoval(): Unit =
      with(mutableByteGapBufferOf()) {
        push(4)
        assertEquals(1, size)
        assertEquals(4, get(0))
        assertFails { get(1) }
        assertEquals(4, remove(0)[0])
        assertEquals(0, size)
        assertContentEquals(byteArrayOf(), toByteArray())
      }

  @Test
  fun emptyBuffer_threeInsertions_notInOrder(): Unit =
      with(mutableByteGapBufferOf()) {
        push(1, 0)
        push(2, 0)
        push(3)
        assertEquals(2, get(0))
        assertEquals(1, get(1))
        assertEquals(3, get(2))
        assertContentEquals(byteArrayOf(2, 1, 3), toByteArray())
      }

  @Test
  fun emptyBuffer_FourInsertions_array(): Unit =
      with(mutableByteGapBufferOf()) {
        push(4)
        push(byteArrayOf(2, 1, 3), offset = 0)
        assertEquals(4, get(3))
        assertEquals(2, get(0))
        assertEquals(1, get(1))
        assertEquals(3, get(2))
        assertEquals(4, size)
        assertContentEquals(byteArrayOf(2, 1, 3, 4), toByteArray())
      }

  @Test
  fun emptyBuffer_clear_empties(): Unit =
      with(mutableByteGapBufferOf()) {
        push(4)
        clear()
        assertEquals(0, size)
        push(5)
        assertEquals(5, get(0))
        assertEquals(1, size)
        assertContentEquals(byteArrayOf(5), toByteArray())
      }

  @Test
  fun emptyBuffer_insertionsOutOfOrder(): Unit =
      with(mutableByteGapBufferOf()) {
        push(byteArrayOf(1, 2, 3, 4))
        push(byteArrayOf(5, 6, 7, 8), offset = 0)
        assertContentEquals(byteArrayOf(5, 6, 7, 8, 1, 2, 3, 4), toByteArray())
      }
}
