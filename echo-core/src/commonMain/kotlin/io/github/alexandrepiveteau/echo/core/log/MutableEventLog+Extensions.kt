package io.github.alexandrepiveteau.echo.core.log

/**
 * Returns true iff there are no events in the [MutableEventLog].
 *
 * @see MutableEventLog.size
 */
fun MutableEventLog.isEmpty(): Boolean {
  return size == 0
}

/**
 * Returns true iff there are some events in the [MutableEventLog].
 *
 * @see MutableEventLog.size
 */
fun MutableEventLog.isNotEmpty(): Boolean {
  return size != 0
}

/** Returns a copy of the current [MutableEventLog]. */
fun MutableEventLog.copyOf(): MutableEventLog {
  return merge(mutableEventLogOf())
}
