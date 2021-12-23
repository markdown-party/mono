package io.github.alexandrepiveteau.echo

import kotlinx.coroutines.CoroutineScope

/**
 * Runs the provided suspending function as a top-level test. The body has a [CoroutineScope].
 *
 * @param f the suspending function to test.
 */
expect fun suspendTest(f: suspend CoroutineScope.() -> Unit)
