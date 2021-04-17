package io.github.alexandrepiveteau.echo.internal.history

import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.EventLog.IndexedEvent
import io.github.alexandrepiveteau.echo.projections.Step
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection
import kotlin.test.Test
import kotlin.test.assertEquals

class ActualPersistentHistoryTest {

  /** A test that verifies that inserting a new event works well. */
  @Test
  fun emptyHistory_supportsAddition() {
    val projection = ActualPersistentHistory(initial = emptyList(), projection = ListProjection)

    val result = projection.forward(HistoryEvent(SiteIdentifier(1), SequenceNumber.Zero, 1))

    assertEquals(listOf(1), result.first.current.model)
  }

  @Test
  fun emptyHistory_supportsForward() {
    var projection: PersistentLogHistory<Int, List<Int>, Int> =
        ActualPersistentHistory(
            initial = emptyList(),
            projection = ListProjection,
        )

    for (i in 0..10) {
      projection =
          projection.forward(
                  HistoryEvent(
                      site = SiteIdentifier(1),
                      seqno = SequenceNumber(i.toUInt()),
                      body = i,
                  ))
              .first
    }
    assertEquals((0..10).toList(), projection.current.model)
  }

  @Test
  fun outOfOrderEvents_areReordered() {
    var projection: PersistentLogHistory<Int, List<Int>, Int> =
        ActualPersistentHistory(
            initial = emptyList(),
            projection = ListProjection,
        )

    for (i in (0..10) - 5) {
      projection =
          projection.forward(
                  HistoryEvent(
                      site = SiteIdentifier(1),
                      seqno = SequenceNumber(i.toUInt()),
                      body = i,
                  ))
              .first
    }
    projection =
        projection.forward(
                HistoryEvent(
                    site = SiteIdentifier(1),
                    seqno = SequenceNumber(5.toUInt()),
                    body = 5,
                ))
            .first
    assertEquals((0..10).toList(), projection.current.model)
  }

  @Test
  fun addToBeginning_successfullyReorders() {
    var projection: PersistentLogHistory<Int, List<Int>, Int> =
        ActualPersistentHistory(
            initial = emptyList(),
            projection = ListProjection,
        )

    for (i in 0..3) {
      projection =
          projection.forward(
                  HistoryEvent(
                      site = SiteIdentifier(1),
                      seqno = SequenceNumber(i.toUInt() + 1u),
                      body = i + 1,
                  ))
              .first
    }
    projection =
        projection.forward(
                HistoryEvent(
                    site = SiteIdentifier(1),
                    seqno = SequenceNumber(0u),
                    body = 0,
                ))
            .first
    assertEquals((0..4).toList(), projection.current.model)
  }
}

// TESTING UTILITIES

/**
 * A [TwoWayProjection] that appends events at the end of the a [List]. This is a great way to check
 * that the history is in the right order.
 *
 * Additionally, changes (which consist in deleting an element) require that the specified element
 * is indeed present at the end of the list.
 */
private val ListProjection =
    object : TwoWayProjection<List<Int>, IndexedEvent<Int>, Int> {

      override fun forward(
          body: IndexedEvent<Int>,
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
