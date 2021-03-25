package markdown.echo.demo.register

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import markdown.echo.MutableSite
import markdown.echo.causal.SiteIdentifier
import markdown.echo.mutableSite
import markdown.echo.projections.OneWayProjection
import markdown.echo.projections.projection
import markdown.echo.sync

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
private class LWWProjection<T> : OneWayProjection<T?, LWWRegisterEvent<T>> {

  override fun forward(body: LWWRegisterEvent<T>, model: T?): T? =
      when (body) {
        // Always pick the latest event value, which has the highest event identifier.
        is LWWRegisterEvent.Set -> body.value
      }
}

/** A class representing a [LWWRegister]. */
private class LWWRegister<T>(private val echo: MutableSite<LWWRegisterEvent<T>>) {

  /** The latest available value from the [LWWRegister]. */
  val value: Flow<T?> = echo.projection(null, LWWProjection())

  suspend fun set(value: T) {
    // By default, events are added with a highest seqno than whatever they've received until now.
    echo.event { yield(LWWRegisterEvent.Set(value)) }
  }
}

class LWWRegisterTest {

  @Test
  fun `two sites eventually converge on a LWW value`(): Unit = runBlocking {
    val alice = SiteIdentifier(123)
    val aliceExchange = mutableSite<LWWRegisterEvent<Int>>(alice)
    val aliceRegister = LWWRegister(aliceExchange)

    val bob = SiteIdentifier(456)
    val bobExchange = mutableSite<LWWRegisterEvent<Int>>(bob)
    val bobRegister = LWWRegister(bobExchange)

    aliceRegister.set(123)
    bobRegister.set(456)

    assertEquals(123, aliceRegister.value.filterNotNull().first())
    assertEquals(456, bobRegister.value.filterNotNull().first())

    // Sync for a bit.
    withTimeoutOrNull(1000) { sync(aliceExchange, bobExchange) }

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
    withTimeoutOrNull(1000) { sync(aliceExchange, bobExchange) }

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
