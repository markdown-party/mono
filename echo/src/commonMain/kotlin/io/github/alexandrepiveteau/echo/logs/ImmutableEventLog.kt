package io.github.alexandrepiveteau.echo.logs

/**
 * An immutable variant of [EventLog].
 *
 * @param T the type of of the body of one event.
 */
interface ImmutableEventLog<out T> : EventLog<T> {

  /** Transforms this [ImmutableEventLog] to a persistable instance. */
  fun toPersistentEventLog(): PersistentEventLog<T>
}
