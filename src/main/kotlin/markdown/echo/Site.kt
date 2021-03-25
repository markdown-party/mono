package markdown.echo

import markdown.echo.Message.V1.Incoming as Inc
import markdown.echo.Message.V1.Outgoing as Out
import markdown.echo.causal.SiteIdentifier
import markdown.echo.events.EventScope
import markdown.echo.logs.MutableEventLogSite
import markdown.echo.memory.log.EventLog
import markdown.echo.memory.log.MutableEventLog
import markdown.echo.memory.log.mutableEventLogOf

/**
 * An interface describing a [Site] in the distributed system. Each [Site] is associated with a
 * globally unique [SiteIdentifier].
 *
 * @param T the type of the events managed by this [Site].
 */
interface Site<T> : Exchange<Inc<T>, Out<T>> {
  val identifier: SiteIdentifier
}

/**
 * A mutable version of [Site], which allows the insertion of the events [T] through its [event]
 * method.
 *
 * @param T the type of the events managed by this [Site].
 */
interface MutableSite<T> : Site<T> {

  /**
   * Creates some new events, that are generated in the [EventScope]. This function returns once the
   * events have been successfully added to the underlying [MutableSite].
   */
  suspend fun event(scope: suspend EventScope<T>.(EventLog<T>) -> Unit)
}

/**
 * Creates a new [Site] for the provided [SiteIdentifier], which can not be manually mutated.
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param T the type of the events managed by this [Site].
 */
fun <T> site(identifier: SiteIdentifier): Site<T> = mutableSite(identifier)

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing [log].
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param log the underlying [MutableEventLog] for this [MutableSite].
 *
 * @param T the type of the events managed by this [Site].
 */
fun <T> mutableSite(
    identifier: SiteIdentifier,
    log: MutableEventLog<T> = mutableEventLogOf(),
): MutableSite<T> = MutableEventLogSite(identifier, log)
