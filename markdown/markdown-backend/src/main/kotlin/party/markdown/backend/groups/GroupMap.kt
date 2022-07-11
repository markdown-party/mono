package party.markdown.backend.groups

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import party.markdown.backend.SessionIdentifier

/**
 * A map of all the collaboration sessions which are currently underway. Each [Group] can be
 * accessed using a unique identifier, and it is guaranteed that a single [Group] will be created
 * for all the clients which request access using the same session identifier.
 *
 * @param scope the [CoroutineScope] in which the [GroupMap] is running.
 */
class GroupMap(private val scope: CoroutineScope) {

  /** The [Mutex] that protects the [groups]. */
  private val mutex = Mutex()

  /** The [Group]s, keyed by session identifier. */
  private val groups = mutableMapOf<SessionIdentifier, Group>()

  /**
   * Returns the [Group] associated to the given [SessionIdentifier].
   *
   * @param session the [SessionIdentifier] that uniquely identifies this group.
   * @return the [Group] for this collaboration session.
   */
  suspend fun get(
      session: SessionIdentifier,
  ): Group = mutex.withLock { groups.getOrPut(session) { Group(scope) } }
}
