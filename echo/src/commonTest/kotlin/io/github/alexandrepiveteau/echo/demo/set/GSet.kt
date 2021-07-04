package io.github.alexandrepiveteau.echo.demo.set

import io.github.alexandrepiveteau.echo.DefaultSerializationFormat
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.mutableHistoryOf
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.projections.ChangeScope
import io.github.alexandrepiveteau.echo.projections.TwoWayMutableProjection
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection
import io.github.alexandrepiveteau.echo.suspendTest
import io.github.alexandrepiveteau.echo.sync
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable

@Serializable
private sealed class GSetEvent {
  @Serializable data class Add(val item: Int) : GSetEvent()
}

@Serializable
private sealed class GSetChange {
  @Serializable data class Insert(val item: Int) : GSetChange()
}

private class GSetProjection : TwoWayProjection<Set<Int>, GSetEvent, GSetChange> {

  override fun ChangeScope<GSetChange>.forward(
      model: Set<Int>,
      id: EventIdentifier,
      event: GSetEvent,
  ): Set<Int> =
      when (event) {
        is GSetEvent.Add ->
            if (event.item in model) model
            else {
              push(GSetChange.Insert(event.item))
              model + event.item
            }
      }

  override fun backward(
      model: Set<Int>,
      id: EventIdentifier,
      event: GSetEvent,
      change: GSetChange,
  ): Set<Int> =
      when (change) {
        is GSetChange.Insert -> model - change.item
      }
}

class GSetTest {

  @Test
  fun oneSite_canYieldEvents() = suspendTest {
    val alice = Random.nextSiteIdentifier()
    val echo =
        mutableSite(
            identifier = alice,
            initial = emptySet(),
            projection = GSetProjection(),
        )

    echo.event {
      yield(GSetEvent.Add(1))
      yield(GSetEvent.Add(2))
      yield(GSetEvent.Add(3))
    }

    val result = echo.value.first { it.size == 3 }
    assertEquals(setOf(1, 2, 3), result)
  }

  @Test
  fun reproducer_b84() {
    val a = Random.nextSiteIdentifier()
    val b = Random.nextSiteIdentifier()
    val format = DefaultSerializationFormat

    val history =
        mutableHistoryOf(
            emptySet(),
            TwoWayMutableProjection(
                GSetProjection(),
                GSetChange.serializer(),
                GSetEvent.serializer(),
                format,
            ),
        )

    val serializer = GSetEvent.serializer()

    history.append(a, format.encodeToByteArray(serializer, GSetEvent.Add(1)))
    history.append(a, format.encodeToByteArray(serializer, GSetEvent.Add(2)))

    history.insert(
        seqno = SequenceNumber.Min + 0u,
        site = b,
        event = format.encodeToByteArray(serializer, GSetEvent.Add(2)),
    )

    assertEquals(setOf(1, 2), history.current)
  }

  @Test
  fun twoSites_converge() = suspendTest {
    // Create Alice, our first site.
    val alice = Random.nextSiteIdentifier()
    val aliceEcho =
        mutableSite(
            identifier = alice,
            initial = emptySet(),
            projection = GSetProjection(),
        )

    // Create Bob, our second site.
    val bob = Random.nextSiteIdentifier()
    val bobEcho =
        mutableSite(
            identifier = bob,
            initial = emptySet(),
            projection = GSetProjection(),
        )

    // Alice adds the elements 1 and 2.
    aliceEcho.event {
      yield(GSetEvent.Add(1))
      yield(GSetEvent.Add(2))
    }

    // Bob adds the elements 2, 3 and 4 concurrently.
    bobEcho.event {
      yield(GSetEvent.Add(2))
      yield(GSetEvent.Add(3))
      yield(GSetEvent.Add(4))
    }

    val aliceBeforeSync = aliceEcho.value.first { it.size == 2 }
    val bobBeforeSync = bobEcho.value.first { it.size == 3 }

    // Before sync, both sites have not merged their operations yet.
    assertEquals(setOf(1, 2), aliceBeforeSync)
    assertEquals(setOf(2, 3, 4), bobBeforeSync)

    // Sync both sites (with a timeout, since by default they'll keep the connection open until
    // either side cancels).
    try {
      withTimeout(timeMillis = 1000) { sync(aliceEcho, bobEcho) }
    } catch (expect: TimeoutCancellationException) {}

    // Finally, look at the resulting set of both sites.
    val aliceAfterSync = aliceEcho.value.first { it.size == 4 }
    val bobAfterSync = aliceEcho.value.first { it.size == 4 }

    // All the events are properly synced.
    val expected = setOf(1, 2, 3, 4)
    assertEquals(expected, aliceAfterSync)
    assertEquals(expected, bobAfterSync)
  }
}
