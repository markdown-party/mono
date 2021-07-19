package party.markdown

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

// Actual JVM-compatible implementation of a top-level suspending function.
actual fun suspendTest(f: suspend CoroutineScope.() -> Unit) = runBlocking { f() }
