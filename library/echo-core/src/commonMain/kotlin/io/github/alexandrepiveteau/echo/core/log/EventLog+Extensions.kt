package io.github.alexandrepiveteau.echo.core.log

/**
 * Returns true iff there are no events in the [EventLog].
 *
 * @see EventLog.size
 */
fun EventLog.isEmpty(): Boolean {
  return size == 0
}

/**
 * Returns true iff there are some events in the [EventLog].
 *
 * @see EventLog.size
 */
fun EventLog.isNotEmpty(): Boolean {
  return size != 0
}

/** Returns a copy of the current [EventLog]. */
fun EventLog.copyOf(): MutableEventLog {
  return mutableEventLogOf().merge(this)
}
