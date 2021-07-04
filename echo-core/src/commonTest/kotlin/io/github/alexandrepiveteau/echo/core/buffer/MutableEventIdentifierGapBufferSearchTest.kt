package io.github.alexandrepiveteau.echo.core.buffer

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber.Companion.Min
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MutableEventIdentifierGapBufferSearchTest {

  @Test
  fun test_FindsRange() {
    val count = 1000u
    val buffer =
        mutableEventIdentifierGapBufferOf().apply {
          for (i in 0u..count) {
            push(EventIdentifier(Min + i, SiteIdentifier.Max))
          }
        }

    for (i in 0u..count) {
      val id = EventIdentifier(Min + i, SiteIdentifier.Min)
      val index = i.toInt()
      assertEquals(index, -(buffer.binarySearch(id) + 1)) // inverted index
      assertEquals(index, buffer.linearSearch(id))
    }
  }

  @Test
  fun terminates_identicalValues() {
    val count = 1000
    val identifier = EventIdentifier(Min, SiteIdentifier.Min)
    val buffer = MutableEventIdentifierGapBuffer(count) { identifier }
    assertTrue(buffer.binarySearch(identifier) in 0..count)
    assertTrue(buffer.linearSearch(identifier) in 0..count)
  }
}
