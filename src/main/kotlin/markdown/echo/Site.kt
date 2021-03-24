package markdown.echo

import markdown.echo.Message.V1.Incoming as Inc
import markdown.echo.Message.V1.Outgoing as Out
import markdown.echo.causal.SiteIdentifier
import markdown.echo.events.EventScope
import markdown.echo.memory.log.EventLog

/**
 * An interface describing a [Site] in the distributed system. Each [Site] is associated with a
 * globally unique [SiteIdentifier].
 *
 * @param T the type of the operations managed by this [Site].
 */
interface Site<T> : Exchange<Inc<T>, Out<T>> {
  val identifier: SiteIdentifier
}

/**
 * A mutable version of [Site], which allows the insertion of the events [T] through its [event]
 * method.
 *
 * @param T the type of the operations managed by this [Site].
 */
interface MutableSite<T> : Site<T> {

  /**
   * Creates some new events, that are generated in the [EventScope]. This function returns once the
   * events have been successfully added to the underlying [MutableSite].
   */
  suspend fun event(scope: suspend EventScope<T>.(EventLog<T>) -> Unit)
}
