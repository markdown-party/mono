package markdown.echo

import markdown.echo.Message.V1.Incoming as Inc
import markdown.echo.Message.V1.Outgoing as Out
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SiteIdentifier
import markdown.echo.events.EventScope
import markdown.echo.logs.EventLog
import markdown.echo.logs.MutableEventLog
import markdown.echo.logs.internal.NoProjectionSite
import markdown.echo.logs.internal.OneWayProjectionSite
import markdown.echo.logs.mutableEventLogOf
import markdown.echo.projections.OneWayProjection

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
 * @param M the type of the underlying aggregated model for this [Site].
 */
interface MutableSite<T, M> : Site<T> {

  /**
   * Creates some new events, that are generated in the [EventScope]. This function returns once the
   * events have been successfully added to the underlying [MutableSite].
   */
  suspend fun event(scope: suspend EventScope<T>.(M) -> Unit)
}

/**
 * A typealias for [MutableSite] that simply expose the underlying [EventLog].
 *
 * @param T the type of the events managed by this [MutableSite].
 */
typealias MutableEventLogSite<T> = MutableSite<T, EventLog<T>>

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
): MutableSite<T, EventLog<T>> = NoProjectionSite(identifier, log)

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing [log].
 * Additionally, this overload takes a [OneWayProjection] and lets you specify a projection to apply
 * to the data, to have custom [MutableSite.event] arguments.
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param log the underlying [MutableEventLog] for this [MutableSite].
 * @param initial the initial value for the projection aggregate.
 * @param projection the [OneWayProjection] for this [Site].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 */
fun <M, T> mutableSite(
    identifier: SiteIdentifier,
    log: MutableEventLog<T> = mutableEventLogOf(),
    initial: M,
    projection: OneWayProjection<M, T>
): MutableSite<T, M> =
    mutableSiteWithIdentifier(identifier, log, initial) { body, acc ->
      projection.forward(body.second, acc)
    }

// TODO : Eventually expose this, with a better name.
internal fun <M, T> mutableSiteWithIdentifier(
    identifier: SiteIdentifier,
    log: MutableEventLog<T> = mutableEventLogOf(),
    initial: M,
    projection: OneWayProjection<M, Pair<EventIdentifier, T>>,
): MutableSite<T, M> = OneWayProjectionSite(identifier, log, initial, projection)
