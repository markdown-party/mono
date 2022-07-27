package io.github.alexandrepiveteau.echo.projections

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier

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
public interface TwoWayProjection<M, in T, C> {

  /**
   * Applies the event to the given [model], and returns a new immutable model. Additionally,
   * changes should be issued using the [ChangeScope].
   *
   * @receiver the [ChangeScope] to issue some changes. You should note that the [backward] method
   * will only be called for changes issued to the [ChangeScope], so unless your events are
   * commutative, you should make sure to save them there.
   *
   * @param model the current model.
   * @param id the [EventIdentifier] of the event.
   * @param event the body of the event.
   *
   * @return the updated model.
   */
  public fun ChangeScope<C>.forward(
      model: M,
      id: EventIdentifier,
      event: T,
  ): M

  /**
   * Reverses a change issued during [forward] traversal.
   *
   * @param model the current model.
   * @param id the [EventIdentifier] of the event.
   * @param event the body of the event.
   * @param change the body of the change.
   *
   * @return the updated model.
   */
  public fun backward(
      model: M,
      id: EventIdentifier,
      event: T,
      change: C,
  ): M
}
