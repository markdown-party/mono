package io.github.alexandrepiveteau.echo.demo.register

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.EventLog.IndexedEvent
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import io.github.alexandrepiveteau.echo.suspendTest
import io.github.alexandrepiveteau.echo.sync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A simple LWW register, where concurrent writes are resolved by looking at the highest absolute
 * timestamp.
 */
private sealed class LWWRegisterEvent<out T> {

  /** Sets the [value] in the register. */
  data class Set<out T>(
      val value: T,
  ) : LWWRegisterEvent<T>()
}
/** Aggregates the [LWWProjection] events. */
private class LWWProjection<T> : OneWayProjection<T?, IndexedEvent<LWWRegisterEvent<T>>> {

  override fun forward(body: IndexedEvent<LWWRegisterEvent<T>>, model: T?): T? =
      when (val event = body.body) {
        // Always pick the latest event value, which has the highest event identifier.
        is LWWRegisterEvent.Set -> event.value
      }
}

/** A class representing a [LWWRegister]. */
private class LWWRegister<T>(
    val exchange: MutableSite<LWWRegisterEvent<T>, T?>,
) {

  /** The latest available value from the [LWWRegister]. */
  val value: Flow<T?> = exchange.value

  suspend fun set(value: T) {
    // By default, events are added with a highest seqno than whatever they've received until now.
    exchange.event { yield(LWWRegisterEvent.Set(value)) }
  }
}

class LWWRegisterTest {

  @Test
  fun twoSites_converge(): Unit = suspendTest {
    val alice = SiteIdentifier(123)
    val aliceRegister = LWWRegister<Int>(mutableSite(alice, null, projection = LWWProjection()))

    val bob = SiteIdentifier(456)
    val bobRegister = LWWRegister<Int>(mutableSite(bob, null, projection = LWWProjection()))

    aliceRegister.set(123)
    bobRegister.set(456)

    assertEquals(123, aliceRegister.value.filterNotNull().first())
    assertEquals(456, bobRegister.value.filterNotNull().first())

    // Sync for a bit.
    withTimeoutOrNull(1000) { sync(aliceRegister.exchange, bobRegister.exchange) }

    // Ensure convergence over a non-null value.
    val shared =
        combine(
                aliceRegister.value.filterNotNull(),
                bobRegister.value.filterNotNull(),
            ) { a, b -> a to b }
            .filter { (a, b) -> a == b }
            .map { (a, _) -> a }
            .first()

    // Let the "other" site issue an event.
    val register =
        when (shared) {
          123 -> bobRegister
          456 -> aliceRegister
          else -> fail("Expected convergence over 123 or 456.")
        }

    // Set the shared value and sync a bit.
    register.set(789)
    withTimeoutOrNull(1000) { sync(aliceRegister.exchange, bobRegister.exchange) }

    // Ensure convergence over a non-null value.
    val result =
        combine(
                aliceRegister.value.filterNotNull(),
                bobRegister.value.filterNotNull(),
            ) { a, b -> a to b }
            .filter { (a, b) -> a == b }
            .map { (a, _) -> a }
            .first()

    assertEquals(789, result)
  }
}
