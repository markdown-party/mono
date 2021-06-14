package io.github.alexandrepiveteau.echo.core.buffer

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFails

class MutableByteGapBufferCopyIntoTest {

  @Test
  fun empty() {
    with(mutableByteGapBufferOf()) { assertContentEquals(byteArrayOf(), copyInto(ByteArray(0))) }
  }

  @Test
  fun outOfBounds() {
    with(MutableByteGapBuffer(3) { it.toByte() }) {
      gap.shift(-1) // just spicing up the tests a bit
      assertFails { copyInto(ByteArray(3), destinationOffset = -1) }
      assertFails { copyInto(ByteArray(3), destinationOffset = 1) }
      assertFails { copyInto(ByteArray(3), destinationOffset = 4) }
      assertFails { copyInto(ByteArray(3), startOffset = 1, endOffset = 4) }
      assertFails { copyInto(ByteArray(2)) }
    }
  }

  @Test
  fun copyAll() {
    with(MutableByteGapBuffer(3) { it.toByte() }) {
      gap.shift(-1)
      assertContentEquals(byteArrayOf(0, 1, 2), copyInto(ByteArray(3)))
      assertContentEquals(byteArrayOf(0, 1, 2, 0), copyInto(ByteArray(4)))
      assertContentEquals(byteArrayOf(0, 0, 1, 2), copyInto(ByteArray(4), 1))
    }
  }

  @Test
  fun copyPart() {
    with(MutableByteGapBuffer(3) { it.toByte() }) {
      gap.shift(-1)
      assertContentEquals(
          byteArrayOf(7, 0),
          copyInto(
              bytes = ByteArray(2) { 7 },
              destinationOffset = 1,
              startOffset = 0,
              endOffset = 1,
          ))
      assertContentEquals(
          byteArrayOf(7, 2),
          copyInto(
              bytes = ByteArray(2) { 7 },
              destinationOffset = 1,
              startOffset = 2,
              endOffset = 3,
          ),
      )
      assertContentEquals(byteArrayOf(0, 1), copyInto(ByteArray(2), endOffset = 2))
      assertContentEquals(byteArrayOf(1, 2), copyInto(ByteArray(2), startOffset = 1))
    }
  }
}
