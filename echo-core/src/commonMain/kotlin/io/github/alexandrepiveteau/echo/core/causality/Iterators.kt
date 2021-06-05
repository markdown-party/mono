package io.github.alexandrepiveteau.echo.core.causality

// TODO : Document this class.
interface EventIdentifierIterator : Iterator<EventIdentifier> {
  override fun next(): EventIdentifier = nextEventIdentifier()
  fun nextEventIdentifier(): EventIdentifier
}
