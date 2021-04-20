package io.github.alexandrepiveteau.echo.internal.flow

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

/** Maps a [StateFlow] with the given function [f] */
internal fun <F, T> StateFlow<F>.map(
    f: (F) -> T,
): StateFlow<T> = MappingStateFlow(this, f)

/**
 * An implementation of [StateFlow] that maps a [backing] [StateFlow] with the function [f].
 *
 * @param F the type of the items of the [backing] [StateFlow].
 * @param T the type of the items of this [StateFlow].
 */
private class MappingStateFlow<in F, out T>(
    private val backing: StateFlow<F>,
    private val f: (F) -> T,
) : StateFlow<T> {

  @InternalCoroutinesApi
  override suspend fun collect(
      collector: FlowCollector<T>,
  ) =
      backing.collect(
          object : FlowCollector<F> {
            override suspend fun emit(value: F) {
              collector.emit(f(value))
            }
          },
      )

  override val replayCache: List<T>
    get() = backing.replayCache.map(f)

  override val value: T
    get() = f(backing.value)
}
