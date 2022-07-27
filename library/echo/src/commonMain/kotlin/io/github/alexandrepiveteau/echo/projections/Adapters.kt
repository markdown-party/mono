@file:OptIn(ExperimentalSerializationApi::class)

package io.github.alexandrepiveteau.echo.projections

import io.github.alexandrepiveteau.echo.core.buffer.MutableByteGapBuffer
import io.github.alexandrepiveteau.echo.core.buffer.copyOfRange
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.log.ChangeScope as CoreChangeScope
import io.github.alexandrepiveteau.echo.core.log.MutableProjection
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer

/**
 * An adapter that wraps a [CoreChangeScope] and returns a [ChangeScope] for a generic type [T].
 *
 * @param serializer the [KSerializer] for the changes.
 * @param scope the [CoreChangeScope] that's wrapped.
 * @param format the [BinaryFormat] to use to store events.
 *
 * @param T the type of the changes.
 */
private class ChangeScopeAdapter<in T>(
    private val serializer: KSerializer<T>,
    var scope: CoreChangeScope,
    private val format: BinaryFormat,
) : ChangeScope<T> {

  override fun push(
      value: T,
  ) = scope.push(format.encodeToByteArray(serializer, value))
}

/**
 * An implementation of a [MutableProjection] which wraps a [TwoWayProjection] with some dedicated
 * serializers. This lets consumers of [MutableProjection] offer a higher-level API which works on
 * generic types, rather than sequences of bytes.
 *
 * @param projection the backing [OneWayProjection].
 * @param eventSerializer the [KSerializer] for the events.
 * @param format the [BinaryFormat] to use to store events.
 *
 * @param M the type of the model.
 * @param T the type of the events.
 */
public class OneWayMutableProjection<M, T>(
    private val projection: OneWayProjection<M, T>,
    private val eventSerializer: KSerializer<T>,
    private val format: BinaryFormat,
) : MutableProjection<M> {

  override fun CoreChangeScope.forward(
      model: M,
      identifier: EventIdentifier,
      data: MutableByteGapBuffer,
      from: Int,
      until: Int
  ): M =
      projection.forward(
          model = model,
          identifier = identifier,
          event = format.decodeFromByteArray(eventSerializer, data.copyOfRange(from, until)),
      )

  override fun backward(
      model: M,
      identifier: EventIdentifier,
      data: MutableByteGapBuffer,
      from: Int,
      until: Int,
      changeData: MutableByteGapBuffer,
      changeFrom: Int,
      changeUntil: Int
  ): M = model
}

/**
 * An implementation of a [MutableProjection] which wraps a [TwoWayProjection] with some dedicated
 * serializers. This lets consumers of [MutableProjection] offer a higher-level API which works on
 * generic types, rather than sequences of bytes.
 *
 * @param projection the backing [TwoWayProjection].
 * @param changeSerializer the [KSerializer] for the changes.
 * @param eventSerializer the [KSerializer] for the events.
 * @param format the [BinaryFormat] to use to store events.
 *
 * @param M the type of the model.
 * @param T the type of the events.
 * @param C the type of the changes.
 */
public class TwoWayMutableProjection<M, T, C>(
    private val projection: TwoWayProjection<M, T, C>,
    private val changeSerializer: KSerializer<C>,
    private val eventSerializer: KSerializer<T>,
    private val format: BinaryFormat,
) : MutableProjection<M> {

  // Optimization : reuse a single ChangeScopeAdapter instance, rather than allocating one on each
  // forward call.
  private var reused: ChangeScopeAdapter<C>? = null

  /**
   * Retrieves a pooled [ChangeScopeAdapter] that may be used with the given [CoreChangeScope]. A
   * new instance will only be created the first time the [ChangeScopeAdapter] is requested.
   *
   * @param scope the [CoreChangeScope] to use.
   *
   * @return a (not thread-safe) pooled [ChangeScopeAdapter].
   */
  private fun scope(scope: CoreChangeScope): ChangeScopeAdapter<C> {
    val impl = reused ?: ChangeScopeAdapter(changeSerializer, scope, format)

    // Update the required fields.
    impl.scope = scope
    reused = impl

    // Return the reused instance.
    return impl
  }

  override fun CoreChangeScope.forward(
      model: M,
      identifier: EventIdentifier,
      data: MutableByteGapBuffer,
      from: Int,
      until: Int,
  ): M =
      with(projection) {
        scope(this@forward)
            .forward(
                model = model,
                id = identifier,
                event = format.decodeFromByteArray(eventSerializer, data.copyOfRange(from, until)),
            )
      }

  override fun backward(
      model: M,
      identifier: EventIdentifier,
      data: MutableByteGapBuffer,
      from: Int,
      until: Int,
      changeData: MutableByteGapBuffer,
      changeFrom: Int,
      changeUntil: Int,
  ): M =
      projection.backward(
          model = model,
          id = identifier,
          event =
              format.decodeFromByteArray(
                  eventSerializer,
                  data.copyOfRange(from, until),
              ),
          change =
              format.decodeFromByteArray(
                  changeSerializer,
                  changeData.copyOfRange(changeFrom, changeUntil),
              ),
      )
}
