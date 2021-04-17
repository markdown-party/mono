package io.github.alexandrepiveteau.echo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun suspendTest(f: suspend CoroutineScope.() -> Unit): dynamic {
  return GlobalScope.promise { f() }
}
