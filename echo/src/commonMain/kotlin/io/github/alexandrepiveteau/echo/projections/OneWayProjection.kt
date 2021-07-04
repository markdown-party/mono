package io.github.alexandrepiveteau.echo.projections

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier

/**
 * A [OneWayProjection] applies a sequence of events of type [T] to a model of type [M]. This is the
 * most simple projection possible, since it only applies operations in single direction, in a
 * commutative fashion.
 *
 * More specifically, events may be applied more than once, and will necessarily be applied in
 * commutative fashion. The events should therefore be **idempotent**, **associative** and
 * **commutative**.
 *
 * @param M the type of the model.
 * @param T the type of the events.
 */
fun interface OneWayProjection<M, in T> {

  /** Applies the [event] to the given [model], and returns a new immutable model. */
  fun forward(
      model: M,
      identifier: EventIdentifier,
      event: T,
  ): M
}
