package io.github.alexandrepiveteau.echo.demo

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.EventLog
import io.github.alexandrepiveteau.echo.logs.ImmutableEventLog
import io.github.alexandrepiveteau.echo.logs.immutableEventLogOf
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.projections.OneWayProjection

class Site private constructor() {

  companion object {

    class Factory<T, M>(
        private val initial: M,
        private val projection: OneWayProjection<M, EventLog.Entry<T>>
    ) {

      // Incrementing identifiers, so assumptions about operation ordering can be made in tests.
      operator fun component1() = create(SiteIdentifier(0), initial, projection = projection)
      operator fun component2() = create(SiteIdentifier(1), initial, projection = projection)
      operator fun component3() = create(SiteIdentifier(2), initial, projection = projection)
      operator fun component4() = create(SiteIdentifier(3), initial, projection = projection)
      operator fun component5() = create(SiteIdentifier(4), initial, projection = projection)
    }

    /** Convenient way to create multiple multiple sites, which may be used in testing. */
    fun <T, M> createMemoryEchos(
        initial: M,
        projection: OneWayProjection<M, EventLog.Entry<T>>,
    ) = Factory(initial, projection)

    /**
     * Creates a new [MutableSite] with the provided log and operation type.
     *
     * @param identifier the site identifier.
     */
    fun <T, M> create(
        identifier: SiteIdentifier,
        initial: M,
        log: ImmutableEventLog<T> = immutableEventLogOf(),
        projection: OneWayProjection<M, EventLog.Entry<T>>
    ): MutableSite<T, M> =
        mutableSite(
            identifier = identifier,
            initial = initial,
            log = log,
            projection = projection,
        )
  }
}
