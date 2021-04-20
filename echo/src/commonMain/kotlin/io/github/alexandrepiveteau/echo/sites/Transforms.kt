package io.github.alexandrepiveteau.echo.sites

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.events.EventScope
import io.github.alexandrepiveteau.echo.internal.flow.map

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

  override suspend fun event(
      scope: suspend EventScope<T>.(M2) -> Unit,
  ) = backing.event { m -> scope(this, f(m)) }
}
