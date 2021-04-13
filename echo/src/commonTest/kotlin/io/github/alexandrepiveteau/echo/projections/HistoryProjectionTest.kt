package io.github.alexandrepiveteau.echo.projections

import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.EventLog
import io.github.alexandrepiveteau.echo.logs.EventValueEntry
import io.github.alexandrepiveteau.echo.projections.HistoryProjection.History
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryProjectionTest {

  /** A test that verifies that inserting a new event works well. */
  @Test
  fun `empty history supports addition`() {
    val projection = HistoryProjection(ListProjection)

    val result =
        projection.forward(
            EventValueEntry(EventIdentifier(SequenceNumber.Zero, SiteIdentifier(1)), 1),
            ListHistory(emptyList()),
        )

    assertEquals(listOf(1), result.current)
  }

  @Test
  fun `empty history supports multiple forward`() {
    val projection = HistoryProjection(ListProjection)
    var model = ListHistory(emptyList())
    for (i in 0..10) {
      model =
          model.step(
              site = 1,
              seqno = i,
              event = i,
              projection = projection,
          )
    }
    assertEquals((0..10).toList(), model.current)
  }

  @Test
  fun `out of order events are reordered`() {
    val projection = HistoryProjection(ListProjection)
    var model = ListHistory(emptyList())

    for (i in (0..10) - 5) {
      model =
          model.step(
              site = 1,
              seqno = i,
              event = i,
              projection = projection,
          )
    }
    model = model.step(site = 1, seqno = 5, event = 5, projection = projection)
    assertEquals((0..10).toList(), model.current)
  }

  @Test
  fun `add to start works reordering`() {
    val projection = HistoryProjection(ListProjection)
    var model = ListHistory(emptyList())

    for (i in 0..3) {
      model = model.step(site = 1, seqno = i + 1, event = i + 1, projection = projection)
    }
    model = model.step(site = 1, seqno = 0, event = 0, projection = projection)
    assertEquals((0..4).toList(), model.current)
  }
}

// TESTING UTILITIES

private typealias ListHistory = History<List<Int>, Int, Int>

@OptIn(ExperimentalUnsignedTypes::class)
private fun ListHistory.step(
    site: Int,
    seqno: Int,
    event: Int,
    projection: HistoryProjection<List<Int>, Int, Int>,
): ListHistory =
    projection.forward(
        body =
            EventValueEntry(
                identifier =
                    EventIdentifier(
                        SequenceNumber(seqno.toUInt()),
                        SiteIdentifier(site),
                    ),
                body = event,
            ),
        model = this,
    )

/**
 * A [TwoWayProjection] that appends events at the end of the a [List]. This is a great way to check
 * that the history is in the right order.
 *
 * Additionally, changes (which consist in deleting an element) require that the specified element
 * is indeed present at the end of the list.
 */
private val ListProjection =
    object : TwoWayProjection<List<Int>, EventLog.Entry<Int>, Int> {

      override fun forward(
          body: EventLog.Entry<Int>,
          model: List<Int>,
      ): Step<List<Int>, Int> {
        return Step(
            data = model + body.body,
            change = body.body,
        )
      }

      override fun backward(
          change: Int,
          model: List<Int>,
      ): List<Int> {
        val last = model.lastOrNull()
        require(last == change) { "Invalid change. Expected $change, got ${last}." }
        return model.dropLast(1)
      }
    }
