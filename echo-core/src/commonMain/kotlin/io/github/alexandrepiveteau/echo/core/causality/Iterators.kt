package io.github.alexandrepiveteau.echo.core.causality

/**
 * An [Iterator] that is specialized for [EventIdentifier]. On the JVM, the values will be
 * represented as a `long`.
 */
interface EventIdentifierIterator : Iterator<EventIdentifier> {

  /** Returns the next [EventIdentifier] for the [Iterator]. */
  override fun next(): EventIdentifier = nextEventIdentifier()

  /** Returns the next [EventIdentifier] for the [Iterator]. */
  fun nextEventIdentifier(): EventIdentifier
}
