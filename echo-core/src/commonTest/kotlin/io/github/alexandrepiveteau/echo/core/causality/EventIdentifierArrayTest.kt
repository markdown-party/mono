package io.github.alexandrepiveteau.echo.core.causality

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventIdentifierArrayTest {

  @Test
  fun defaultValue() {
    assertTrue(EventIdentifierArray(1)[0].isUnspecified)
  }

  @Test
  fun sortTest() {
    val v1 = EventIdentifier(SequenceNumber.Min, SiteIdentifier.Min)
    val v2 = EventIdentifier(SequenceNumber.Max, SiteIdentifier.Min)
    val array = EventIdentifierArray(2)
    array[0] = v2
    array[1] = v1
    array.sort()
    assertEquals(v1, array[0])
    assertEquals(v2, array[1])
  }
}
