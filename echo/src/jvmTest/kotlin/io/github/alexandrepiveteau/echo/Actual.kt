package io.github.alexandrepiveteau.echo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

// Actual JVM-compatible implementation of a top-level suspending function.
actual fun suspendTest(f: suspend CoroutineScope.() -> Unit) = runBlocking { f() }

actual inline fun measureTimeMillis(f: () -> Unit): Long {
  return kotlin.system.measureTimeMillis(f)
}
