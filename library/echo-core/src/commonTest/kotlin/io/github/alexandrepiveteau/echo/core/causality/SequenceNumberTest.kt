package io.github.alexandrepiveteau.echo.core.causality

import kotlin.test.Test
import kotlin.test.assertEquals

class SequenceNumberTest {

  @Test
  fun incFromMin_isSameAsPlusOne() {
    val expected = SequenceNumber.Min.plus(1U)
    val actual = SequenceNumber.Min.inc()
    assertEquals(expected, actual)
  }

  @Test
  fun incFromUnspecified_remainsUnspecified() {
    val expected = SequenceNumber.Unspecified
    val actual = SequenceNumber.Unspecified.inc()
    assertEquals(expected, actual)
  }

  @Test
  fun plusFromUnspecified_remainsUnspecified() {
    val expected = SequenceNumber.Unspecified
    val actual = SequenceNumber.Unspecified.plus(1U)
    assertEquals(expected, actual)
  }
}
