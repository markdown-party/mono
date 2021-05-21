package io.github.alexandrepiveteau.echo

import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

@OptIn(DelicateCoroutinesApi::class)
actual fun suspendTest(f: suspend CoroutineScope.() -> Unit): dynamic {
  return GlobalScope.promise { f() }
}

actual inline fun measureTimeMillis(f: () -> Unit): Long {
  val start = window.performance.now()
  f()
  val end = window.performance.now()
  return end.toLong() - start.toLong()
}
