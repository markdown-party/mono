package io.github.alexandrepiveteau.echo.logs

import io.github.alexandrepiveteau.echo.causal.EventIdentifier

/**
 * An interface representing an [Iterator] over elements, which are all uniquely identified with an
 * [EventIdentifier].
 *
 * @param T the type of the items produced by this [Iterator].
 */
interface EventIterator<out T> : Iterator<T> {

  /** Returns true if the iterator has a previous element. */
  fun hasPrevious(): Boolean

  /**
   * Returns the [EventIdentifier] of the element returned by [previous]. The behavior is undefined
   * if [hasPrevious] would return false.
   */
  fun previousIndex(): EventIdentifier

  /** Retrieves the previous element, and moves back. */
  fun previous(): T

  /** Returns true if the iterator has a next element. */
  override fun hasNext(): Boolean

  /**
   * Returns the [EventIdentifier] of the element returned by [next]. The behavior is undefined if
   * [hasNext] would return false.
   */
  fun nextIndex(): EventIdentifier

  /** Returns the next element, and moves forward. */
  override fun next(): T
}
