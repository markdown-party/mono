package io.github.alexandrepiveteau.echo.projections

/**
 * A [ChangeScope] defines operations which may be undertaken when performing the
 * [TwoWayProjection.forward] moves.
 *
 * @param T the type of the changes issued.
 */
interface ChangeScope<in T> {

  /** Pushes a new [value] to the log of changes. */
  fun push(value: T)
}
