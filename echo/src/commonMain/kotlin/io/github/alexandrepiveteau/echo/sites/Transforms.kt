package io.github.alexandrepiveteau.echo.sites

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.events.EventScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

/**
 * Transforms a [MutableSite] to make it return a different kind of model. This may be particularly
 * useful when creating abstractions backed by a [MutableSite] that should not expose some
 * implementation details.
 *
 * @param f the mapping function.
 *
 * @param T the type of the events.
 * @param M1 the type of the original model.
 * @param M2 the type of the transformed model.
 */
fun <T, M1, M2> MutableSite<T, M1>.map(
    f: (M1) -> M2,
): MutableSite<T, M2> = MappingMutableSite(f, this)

/**
 * An implementation of [MutableSite] which maps the model with a dedicated function.
 *
 * @param f the mapping function.
 * @param backing the underlying [MutableSite].
 */
private class MappingMutableSite<T, out M1, out M2>(
    private val f: (M1) -> M2,
    private val backing: MutableSite<T, M1>
) : MutableSite<T, M2> {

  override fun outgoing() = backing.outgoing()
  override fun incoming() = backing.incoming()

  override val identifier = backing.identifier
  override val value = backing.value.map(f)

  override suspend fun <R> event(
      block: suspend EventScope<T>.(M2) -> R,
  ) = backing.event { m -> block(this, f(m)) }
}

/** Maps a [StateFlow] with the given function [f] */
fun <F, T> StateFlow<F>.map(
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
