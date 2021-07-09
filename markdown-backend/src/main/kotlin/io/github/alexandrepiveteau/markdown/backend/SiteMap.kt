package io.github.alexandrepiveteau.markdown.backend

import io.github.alexandrepiveteau.echo.Site
import io.github.alexandrepiveteau.echo.site
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import party.markdown.tree.TreeEvent

/**
 * A [SiteMap] provides safe access to all the [Site] which are managed by this server. Each site is
 * associated with a unique [SessionIdentifier], such that all requests with the same
 * [SessionIdentifier] will result in the same [Site] begin returned.
 *
 * You may perform concurrent requests on the [Site] once you'll have retrieved it.
 */
class SiteMap {

  /** A [Mutex] to ensure exclusive access to the [sites]. */
  private val mutex = Mutex()

  /** A [MutableMap] of the [Site] associated with each [SessionIdentifier]. */
  private val sites = mutableMapOf<SessionIdentifier, Site<*>>()

  /**
   * Returns the [Site] associated with the given [SessionIdentifier]. This method is safe to call
   * from multiple coroutines concurrently.
   *
   * @param identifier the [SessionIdentifier] used to uniquely identify the session.
   */
  suspend fun get(
      identifier: SessionIdentifier,
  ): Site<*> = mutex.withLock { sites.getOrPut(identifier) { site<TreeEvent>() } }
}
