package io.github.alexandrepiveteau.echo.projections

/**
 * A pair consisting of the new model ([data]) and a delta ([change]).
 *
 * @param M the type of the model.
 * @param C the type of the change.
 */
data class Step<out M, out C>(
    val data: M,
    val change: C,
)

/**
 * A [TwoWayProjection] applies a sequence of events of type [T] to a model of type [M]. This is a
 * more advanced projection than [OneWayProjection], because it allows sites to define a custom
 * change type [C] which will contain meta-data to undo the application of an event. Therefore, it
 * applies operations in multiple directions.
 *
 * This is particularly useful if the underlying model can not be efficiently implemented as a
 * persistent data structure.
 *
 * @param M the type of the model.
 * @param T the type of the events.
 * @param C the type of the changes.
 */
interface TwoWayProjection<M, in T, C> {

  /**
   * Applies the event [body] to the given [model], and returns a new immutable model and its
   * associated change.
   */
  fun forward(body: T, model: M): Step<M, C>

  /** Reverses an event on a [model] by applying the given [change]. */
  fun backward(change: C, model: M): M
}
