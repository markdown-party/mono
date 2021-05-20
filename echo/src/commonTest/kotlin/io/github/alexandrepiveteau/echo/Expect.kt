package io.github.alexandrepiveteau.echo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Runs the provided suspending function as a top-level test. The body has a [CoroutineScope].
 *
 * @param f the suspending function to test.
 */
expect fun suspendTest(f: suspend CoroutineScope.() -> Unit)

/** Suspends forever, and only resumes on cancellation. */
suspend fun suspendForever(): Nothing = suspendCancellableCoroutine {}

/**
 * Returns the amount of milliseconds that were necessary to process the code block.
 *
 * @param f the code block to measure.
 */
expect inline fun measureTimeMillis(f: () -> Unit): Long
