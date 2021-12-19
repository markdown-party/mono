package io.github.alexandrepiveteau.echo.demo.register

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.isSpecified
import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import io.github.alexandrepiveteau.echo.sites.map
import io.github.alexandrepiveteau.echo.suspendTest
import io.github.alexandrepiveteau.echo.sync
import io.github.alexandrepiveteau.echo.sync.SyncStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * A simple LWW register, where concurrent writes are resolved by looking at the highest absolute
 * timestamp.
 */
@Serializable
private sealed class LWWRegisterEvent {

  /** Sets the [value] in the register. */
  @Serializable data class Set(val value: Int) : LWWRegisterEvent()
}

/**
 * Aggregates the [LWWProjection] events. The [LWWRegisterEvent.Set] operation is commutative,
 * associate and idempotent, so we can use a [OneWayProjection].
 */
private class LWWProjection : OneWayProjection<Pair<EventIdentifier, Int>, LWWRegisterEvent> {

  override fun forward(
      model: Pair<EventIdentifier, Int>,
      identifier: EventIdentifier,
      event: LWWRegisterEvent,
  ): Pair<EventIdentifier, Int> =
      when (event) {
        is LWWRegisterEvent.Set ->
            if (model.first > identifier) model else identifier to event.value
      }
}

/** A class representing a [LWWRegister]. */
private class LWWRegister(site: SiteIdentifier) {

  /** The backing [exchange] for the [LWWRegister]. */
  val exchange =
      mutableSite(
          identifier = site,
          initial = EventIdentifier.Unspecified to 0,
          projection = LWWProjection(),
          strategy = SyncStrategy.Once,
      )

  /** The latest available value from the [LWWRegister]. */
  val value: StateFlow<Int?> = exchange.map { (id, value) -> value.takeIf { id.isSpecified } }

  suspend fun set(value: Int) {
    // By default, events are added with a highest seqno than whatever they've received until now.
    exchange.event { yield(LWWRegisterEvent.Set(value)) }
  }
}

class LWWRegisterTest {

  @Test
  fun twoSites_converge(): Unit = suspendTest {
    val alice = 123U.toSiteIdentifier()
    val aliceRegister = LWWRegister(alice)

    val bob = 456U.toSiteIdentifier()
    val bobRegister = LWWRegister(bob)

    aliceRegister.set(123)
    bobRegister.set(456)

    assertEquals(123, aliceRegister.value.value)
    assertEquals(456, bobRegister.value.value)

    // Sync once.
    sync(aliceRegister.exchange, bobRegister.exchange)

    // Ensure convergence over a non-null value.
    val aliceValue = aliceRegister.value.value
    val bobValue = bobRegister.value.value
    assertEquals(aliceValue, bobValue)

    // Let the "other" site issue an event.
    val register =
        when (aliceValue) {
          123 -> bobRegister
          456 -> aliceRegister
          else -> fail("Expected convergence over 123 or 456.")
        }

    // Set the shared value and sync once.
    register.set(789)
    sync(aliceRegister.exchange, bobRegister.exchange)

    // Ensure convergence over a non-null value.
    assertEquals(789, aliceRegister.value.value)
    assertEquals(789, bobRegister.value.value)
  }
}
