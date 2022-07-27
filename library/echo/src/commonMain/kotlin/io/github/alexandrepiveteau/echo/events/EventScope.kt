package io.github.alexandrepiveteau.echo.events

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier

/**
 * An interface describing the operations that can be performed when we want to issue some events.
 * Usually, commands will be described as extensions to the [EventScope] interface.
 *
 * @param T the type of the application-specific event data.
 */
public fun interface EventScope<in T> {

  /**
   * Append a new event to the issued operations by this site. A happens-before relationship is
   * established with (at least) all the preceding events on this site; therefore, two subsequent
   * calls to [yield] guarantee that the order of the operations will be preserved.
   *
   * Please note that some events might be concurrently inserted to other sites of the log, and it's
   * therefore not possible to predict the [EventIdentifier] for this event before it will have been
   * [yield].
   *
   * @param event The event that will be added to the log.
   *
   * @return the [EventIdentifier] that's issued for this new event.
   */
  public fun yield(event: T): EventIdentifier

  /**
   * Appends an [Iterator] of events to the operations of this site.
   *
   * @param events the events that will be added to the log.
   */
  public fun yieldAll(events: Iterator<T>): Unit = events.forEach(::yield)

  /**
   * Appends an [Iterable] of events to the operations of this site.
   *
   * @param events the events that will be added to the log.
   */
  public fun yieldAll(events: Iterable<T>): Unit = yieldAll(events.iterator())
}
