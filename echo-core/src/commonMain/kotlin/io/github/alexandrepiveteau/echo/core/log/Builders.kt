package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier

/**
 * Creates a new [MutableHistory], with an aggregate with an [initial] value and a [projection] for
 * incremental changes.
 *
 * @param initial the initial aggregate value.
 * @param projection the [MutableProjection] value.
 *
 * @param T the type of the aggregate.
 */
fun <T> mutableHistoryOf(
    initial: T,
    projection: MutableProjection<T>,
): MutableHistory<T> = MutableHistoryImpl(initial, projection)

/** Creates a new [MutableEventLog], with no aggregate. */
fun mutableEventLogOf(): MutableEventLog = mutableHistoryOf(NoModel, NoProjection)

// An object which represents the absence of an aggregated model.
private object NoModel

// An object which represents the absence of an aggregating projection.
private object NoProjection : MutableProjection<NoModel> {

  override fun ChangeScope.forward(
      model: NoModel,
      identifier: EventIdentifier,
      data: ByteArray,
      from: Int,
      until: Int,
  ): NoModel = NoModel

  override fun backward(
      model: NoModel,
      identifier: EventIdentifier,
      data: ByteArray,
      from: Int,
      until: Int,
      changeData: ByteArray,
      changeFrom: Int,
      changeUntil: Int,
  ): NoModel = NoModel
}
