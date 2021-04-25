package io.github.alexandrepiveteau.echo.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberTest {

  @Test
  fun overflow_UInt() {
    val a = UInt.MAX_VALUE
    val b = 1U
    assertEquals(UInt.MAX_VALUE, a.plusBoundOverflows(b))
  }
}
