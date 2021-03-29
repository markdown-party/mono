package markdown.echo

import markdown.echo.Message.V1.Incoming as Inc
import markdown.echo.Message.V1.Outgoing as Out
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SiteIdentifier
import markdown.echo.events.EventScope
import markdown.echo.logs.ImmutableEventLog
import markdown.echo.logs.PersistentEventLog
import markdown.echo.logs.immutableEventLogOf
import markdown.echo.logs.internal.OneWayProjectionSite
import markdown.echo.logs.persistentEventLogOf
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
interface MutableSite<T, out M> : Site<T> {

  /**
   * Creates some new events, that are generated in the [EventScope]. This function returns once the
   * events have been successfully added to the underlying [MutableSite].
   */
  suspend fun event(scope: suspend EventScope<T>.(M) -> Unit)
}

/**
 * A typealias for [MutableSite] that simply expose the underlying [ImmutableEventLog].
 *
 * @param T the type of the events managed by this [MutableSite].
 */
typealias EventLogSite<T> = MutableSite<T, ImmutableEventLog<T>>

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
 * @param log the underlying [PersistentEventLog] for this [MutableSite].
 *
 * @param T the type of the events managed by this [Site].
 */
fun <T> mutableSite(
    identifier: SiteIdentifier,
    log: ImmutableEventLog<T> = immutableEventLogOf(),
): MutableSite<T, ImmutableEventLog<T>> =
    OneWayProjectionSite(
        identifier = identifier,
        log = log.toPersistentEventLog(),
        initial = persistentEventLogOf(),
    ) { (id, event), model -> model.apply { set(id.seqno, id.site, event) } }

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing [log].
 * Additionally, this overload takes a [OneWayProjection] and lets you specify a projection to apply
 * to the data, to have custom [MutableSite.event] arguments.
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param log the underlying [ImmutableEventLog] for this [MutableSite].
 * @param initial the initial value for the projection aggregate.
 * @param projection the [OneWayProjection] for this [Site].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 */
fun <M, T> mutableSite(
    identifier: SiteIdentifier,
    log: ImmutableEventLog<T> = immutableEventLogOf(),
    initial: M,
    projection: OneWayProjection<M, T>
): MutableSite<T, M> =
    mutableSiteWithIdentifier(identifier, log, initial) { body, acc ->
      projection.forward(body.second, acc)
    }

// TODO : Find a better name.
fun <M, T> mutableSiteWithIdentifier(
    identifier: SiteIdentifier,
    log: ImmutableEventLog<T> = immutableEventLogOf(),
    initial: M,
    projection: OneWayProjection<M, Pair<EventIdentifier, T>>,
): MutableSite<T, M> =
    OneWayProjectionSite(identifier, log.toPersistentEventLog(), initial, projection)
