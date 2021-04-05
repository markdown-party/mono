package markdown.echo.demo

import markdown.echo.MutableSite
import markdown.echo.causal.SiteIdentifier
import markdown.echo.logs.EventValue
import markdown.echo.logs.ImmutableEventLog
import markdown.echo.logs.immutableEventLogOf
import markdown.echo.mutableSite
import markdown.echo.projections.OneWayProjection

class Site private constructor() {

  companion object {

    class Factory<T, M>(
        private val initial: M,
        private val projection: OneWayProjection<M, EventValue<T>>
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
        projection: OneWayProjection<M, EventValue<T>>,
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
        projection: OneWayProjection<M, EventValue<T>>
    ): MutableSite<T, M> =
        mutableSite(
            identifier = identifier,
            initial = initial,
            log = log,
            projection = projection,
        )
  }
}
