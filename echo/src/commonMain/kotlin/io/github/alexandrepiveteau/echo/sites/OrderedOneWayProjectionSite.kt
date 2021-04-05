package io.github.alexandrepiveteau.echo.sites

import io.github.alexandrepiveteau.echo.EchoEventLogPreview
import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.events.EventScope
import io.github.alexandrepiveteau.echo.logs.*
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import kotlinx.coroutines.flow.map

private class PersistentLogOneWayProjection<T> :
    OneWayProjection<PersistentEventLog<T>, EventValue<T>> {

  override fun forward(body: EventValue<T>, model: PersistentEventLog<T>) =
      model.set(body.identifier.seqno, body.identifier.site, body.value)
}

internal class OrderedOneWayProjectionSite<T, M>(
    override val identifier: SiteIdentifier,
    log: ImmutableEventLog<T> = immutableEventLogOf(),
    private val initial: M,
    private val projection: OneWayProjection<M, EventValue<T>>,
) : MutableSite<T, M> {

  // Backing site, with a persistentLogOf aggregate.
  private val backing =
      UnorderedOneWayProjectionSite(
          identifier = identifier,
          log = log.toPersistentEventLog(),
          initial = persistentEventLogOf(),
          projection = PersistentLogOneWayProjection(),
      )

  /** Aggregates the model and returns it mapped to the user-defined model. */
  @OptIn(EchoEventLogPreview::class)
  private fun aggregate(model: ImmutableEventLog<T>): M {
    return model.foldl(initial, projection::forward)
  }

  // Delegated implementation.
  override val value = backing.value.map { aggregate(it) }
  override fun outgoing() = backing.outgoing()
  override fun incoming() = backing.incoming()
  override suspend fun event(scope: suspend EventScope<T>.(M) -> Unit) {
    backing.event scope@{ log ->
      scope.invoke(
          object : EventScope<T> {
            override suspend fun yield(event: T) = this@scope.yield(event)
          },
          aggregate(log),
      )
    }
  }
}
