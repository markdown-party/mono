package io.github.alexandrepiveteau.echo.projections

/**
 * A [ChangeScope] defines operations which may be undertaken when performing the
 * [TwoWayProjection.forward] moves.
 *
 * @param T the type of the changes issued.
 */
public interface ChangeScope<in T> {

  /** Pushes a new [value] to the log of changes. */
  public fun push(value: T)
}
