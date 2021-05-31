package io.github.alexandrepiveteau.echo.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

class PerfTest {

  @Test
  fun unpackTest() {
    assertEquals(0xFF.toByte(), unpackByte1(0xFF shl 24))
    assertEquals(0xFF.toByte(), unpackByte2(0xFF0000))
    assertEquals(0xFF.toByte(), unpackByte3(0xFF00))
    assertEquals(0xFF.toByte(), unpackByte4(0xFF))
  }

  @Test
  fun packTest() {
    assertEquals(0x12345678, packBytes(0x12, 0x34, 0x56, 0x78))
  }

  @Test
  fun testPerf() {
    val time = measureTime {
      repeat(1_000_000) {
        val b1 = unpackByte1(it)
        val b2 = unpackByte2(it)
        val b3 = unpackByte3(it)
        val b4 = unpackByte4(it)
        assertEquals(it, packBytes(b1, b2, b3, b4))
      }
    }
    val speed = 1000.toDouble() * 1_000_000.toDouble() / time.inWholeMilliseconds
    println("Processing $speed items per second")
  }
}
