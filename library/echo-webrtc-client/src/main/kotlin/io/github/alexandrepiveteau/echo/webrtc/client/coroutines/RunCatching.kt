package io.github.alexandrepiveteau.echo.webrtc.client.coroutines

import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlinx.coroutines.CancellationException

/**
 * Calls the specified [block] and returns the encapsulated result if the invocation was successful,
 * catching any [Throwable] exception that was thrown from the [block] function and encapsulating it
 * as a failure.
 *
 * Unlike [runCatching], this function propagates [CancellationException] and can therefore safely
 * be used with coroutines.
 */
internal inline fun <R> runCatchingCancellable(block: () -> R): Result<R> {
  return try {
    success(block())
  } catch (it: Throwable) {
    if (it is CancellationException) throw it else failure(it)
  }
}

/**
 * Calls the specified [block] and returns the encapsulated result if the invocation was successful,
 * catching any [Throwable] exception that was thrown from the [block] function and encapsulating it
 * as a failure.
 *
 * Unlike [runCatching], this function propagates [CancellationException] and can therefore safely
 * be used with coroutines.
 */
internal inline fun <T, R> T.runCatchingCancellable(block: T.() -> R): Result<R> {
  return try {
    success(block())
  } catch (it: Throwable) {
    if (it is CancellationException) throw it else failure(it)
  }
}
