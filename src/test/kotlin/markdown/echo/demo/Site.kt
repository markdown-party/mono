package markdown.echo.demo

import markdown.echo.Echo
import markdown.echo.causal.SiteIdentifier
import markdown.echo.memory.MemoryEcho
import markdown.echo.memory.log.MutableEventLog
import markdown.echo.memory.log.mutableEventLogOf
import markdown.echo.memory.memory

class Site private constructor() {

  companion object {

    class Factory<T> {

      // Incrementing identifiers, so assumptions about operation ordering can be made in tests.
      operator fun component1() = create<T>(SiteIdentifier(0))
      operator fun component2() = create<T>(SiteIdentifier(1))
      operator fun component3() = create<T>(SiteIdentifier(2))
      operator fun component4() = create<T>(SiteIdentifier(3))
      operator fun component5() = create<T>(SiteIdentifier(4))
    }

    /** Convenient way to create multiple multiple sites, which may be used in testing. */
    fun <T> createMemoryEchos() = Factory<T>()

    /**
     * Creates a new [MemoryEcho] with the provided log and operation type.
     *
     * @param identifier the site identifier.
     * @param log the [MutableEventLog] that backs this site.
     */
    fun <T> create(
        identifier: SiteIdentifier,
        log: MutableEventLog<T> = mutableEventLogOf(),
    ): MemoryEcho<T> =
        Echo.memory(
            site = identifier,
            log = log,
        )
  }
}
