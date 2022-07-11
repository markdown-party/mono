package io.github.alexandrepiveteau.echo.demo.set

import io.github.alexandrepiveteau.echo.DefaultBinaryFormat
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.mutableHistoryOf
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.projections.ChangeScope
import io.github.alexandrepiveteau.echo.projections.TwoWayMutableProjection
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import io.github.alexandrepiveteau.echo.sync
import io.github.alexandrepiveteau.echo.sync.SyncStrategy
import io.github.alexandrepiveteau.echo.sync.SyncStrategy.Companion.Continuous
import io.github.alexandrepiveteau.echo.sync.SyncStrategy.Companion.Once
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
  fun oneSite_canYieldEvents() = runTest {
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

    val result = echo.first { it.size == 3 }
    assertEquals(setOf(1, 2, 3), result)
  }

  @Test
  fun reproducer_b84() {
    val a = Random.nextSiteIdentifier()
    val b = Random.nextSiteIdentifier()
    val format = DefaultBinaryFormat

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
  fun twoSites_singleOnceStrategy_converge() = runTest {
    suspend fun test(a: SyncStrategy<Inc, Out>, b: SyncStrategy<Inc, Out>) {
      val alice =
          mutableSite(
              identifier = Random.nextSiteIdentifier(),
              initial = emptySet(),
              projection = GSetProjection(),
              strategy = a,
          )
      val bob =
          mutableSite(
              identifier = Random.nextSiteIdentifier(),
              initial = emptySet(),
              projection = GSetProjection(),
              strategy = b,
          )
      alice.event { yield(GSetEvent.Add(123)) }
      bob.event { yield(GSetEvent.Add(456)) }
      bob.event { yield(GSetEvent.Add(789)) }

      sync(alice, bob)
      val expected = setOf(123, 456, 789)
      assertEquals(expected, alice.value)
      assertEquals(expected, bob.value)
    }

    test(Once, Continuous)
    test(Continuous, Once)
    test(Once, Once)
  }

  @Test
  fun twoSites_converge() = runTest {
    // Create Alice, our first site.
    val alice = Random.nextSiteIdentifier()
    val aliceEcho =
        mutableSite(
            identifier = alice,
            initial = emptySet(),
            projection = GSetProjection(),
            strategy = Once,
        )

    // Create Bob, our second site.
    val bob = Random.nextSiteIdentifier()
    val bobEcho =
        mutableSite(
            identifier = bob,
            initial = emptySet(),
            projection = GSetProjection(),
            strategy = Once,
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

    val aliceBeforeSync = aliceEcho.first { it.size == 2 }
    val bobBeforeSync = bobEcho.first { it.size == 3 }

    // Before sync, both sites have not merged their operations yet.
    assertEquals(setOf(1, 2), aliceBeforeSync)
    assertEquals(setOf(2, 3, 4), bobBeforeSync)

    // Sync both sites.
    sync(aliceEcho, bobEcho)

    // All the events are properly synced.
    val expected = setOf(1, 2, 3, 4)
    assertEquals(expected, aliceEcho.value)
    assertEquals(expected, bobEcho.value)
  }
}
