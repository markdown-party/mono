package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.MutableByteGapBuffer
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier

/**
 * A [MutableProjection] applies events to a model of type [T]. When events are applied with the
 * [forward] method, some changes may be generated and issued to the [ChangeScope]. These changes
 * will then be made available when the [backward] method is called.
 *
 * If multiple changes are issued in the [ChangeScope], the [backward] method will be called once
 * for each of these changes. The provided event will remain the same in both cases.
 */
public interface MutableProjection<T> {

  /**
   * Moves the [MutableProjection] forward, and returns the new instance of [T] that should be used.
   *
   * @receiver the [ChangeScope] where changes should be issued.
   *
   * @param model the instance of [T] that should be updated.
   * @param identifier the [EventIdentifier] for the event.
   *
   * @param data the bytes that define the event.
   * @param from where the [data] should be read from.
   * @param until where the [data] should be read to.
   */
  public fun ChangeScope.forward(
      model: T,
      // Event.
      identifier: EventIdentifier,
      data: MutableByteGapBuffer,
      from: Int,
      until: Int,
  ): T

  /**
   * Moves the [MutableProjection] backward, and returns the new instance of [T] that should be
   * used.
   *
   * @param model the instance of [T] that should be updated.
   * @param identifier the [EventIdentifier] for the event.
   *
   * @param data the bytes that define the event.
   * @param from where the [data] should be read from.
   * @param until where the [data] should be read to.
   *
   * @param changeData the bytes that define the stored change.
   * @param changeFrom where the [changeData] should be read from.
   * @param changeUntil where the [changeData] should be read to.
   */
  public fun backward(
      model: T,
      // Event.
      identifier: EventIdentifier,
      data: MutableByteGapBuffer,
      from: Int,
      until: Int,
      // Change.
      changeData: MutableByteGapBuffer,
      changeFrom: Int,
      changeUntil: Int,
  ): T
}
