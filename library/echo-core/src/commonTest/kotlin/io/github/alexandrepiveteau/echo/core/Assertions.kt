package io.github.alexandrepiveteau.echo.core

/** Asserts that the given [block] throws an exception [T]. Other exceptions will be propagated. */
inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
  var thrown = false
  try {
    block()
  } catch (it: Throwable) {
    if (it !is T) throw it
    thrown = true
  }
  if (!thrown) throw IllegalStateException("${T::class.simpleName} wasn't thrown.")
}
