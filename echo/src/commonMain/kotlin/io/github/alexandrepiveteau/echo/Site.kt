package io.github.alexandrepiveteau.echo

import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.events.EventScope
import io.github.alexandrepiveteau.echo.logs.EventValue
import io.github.alexandrepiveteau.echo.logs.ImmutableEventLog
import io.github.alexandrepiveteau.echo.logs.immutableEventLogOf
import io.github.alexandrepiveteau.echo.logs.persistentEventLogOf
import io.github.alexandrepiveteau.echo.projections.HistoryProjection
import io.github.alexandrepiveteau.echo.projections.HistoryProjection.History
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection
import io.github.alexandrepiveteau.echo.protocol.Message.V1.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.V1.Outgoing as Out
import io.github.alexandrepiveteau.echo.sites.PersistentSite
import io.github.alexandrepiveteau.echo.sites.map
import kotlinx.coroutines.flow.Flow

/**
 * An interface describing a [Site] in the distributed system. Each [Site] is associated with a
 * globally unique [SiteIdentifier].
 *
 * @param T the type of the events managed by this [Site].
 * @param M the type of the underlying aggregated model for this [Site].
 */
interface Site<T, out M> : Exchange<Inc<T>, Out<T>> {
  val identifier: SiteIdentifier
  val value: Flow<M>
}

/**
 * A mutable version of [Site], which allows the insertion of the events [T] through its [event]
 * method.
 *
 * @param T the type of the events managed by this [Site].
 * @param M the type of the underlying aggregated model for this [Site].
 */
interface MutableSite<T, out M> : Site<T, M> {

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
fun <T> site(identifier: SiteIdentifier): Site<T, ImmutableEventLog<T>> = mutableSite(identifier)

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing [log].
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param log the underlying [io.github.alexandrepiveteau.echo.logs.PersistentEventLog] for this
 * [MutableSite].
 *
 * @param T the type of the events managed by this [Site].
 */
fun <T> mutableSite(
    identifier: SiteIdentifier,
    log: ImmutableEventLog<T> = immutableEventLogOf(),
): EventLogSite<T> =
    unorderedSite(
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
    initial: M,
    log: ImmutableEventLog<T> = immutableEventLogOf(),
    projection: OneWayProjection<M, EventValue<T>>,
): MutableSite<T, M> =
    unorderedSite(identifier, History(initial), log, HistoryProjection(projection)).map {
      it.current
    }

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing [log].
 * Additionally, this overload takes a [TwoWayProjection] and lets you specify a projection to apply
 * to the data, to have custom [MutableSite.event] arguments.
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param log the underlying [ImmutableEventLog] for this [MutableSite].
 * @param initial the initial value for the projection aggregate.
 * @param projection the [TwoWayProjection] for this [Site].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 * @param C the type of the changes generated by this [Site].
 */
fun <M, T, C> mutableSite(
    identifier: SiteIdentifier,
    initial: M,
    log: ImmutableEventLog<T> = immutableEventLogOf(),
    projection: TwoWayProjection<M, EventValue<T>, C>,
): MutableSite<T, M> =
    unorderedSite(identifier, History(initial), log, HistoryProjection(projection)).map {
      it.current
    }

// INTERNAL BUILDERS

internal fun <M, T> unorderedSite(
    identifier: SiteIdentifier,
    initial: M,
    log: ImmutableEventLog<T> = immutableEventLogOf(),
    projection: OneWayProjection<M, EventValue<T>>,
): MutableSite<T, M> =
    PersistentSite(identifier, log.toPersistentEventLog(), initial, projection)
