@file:JvmName("Exchanges")
@file:JvmMultifileClass

package io.github.alexandrepiveteau.echo

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.MutableEventLog
import io.github.alexandrepiveteau.echo.core.log.MutableHistory
import io.github.alexandrepiveteau.echo.core.log.mutableEventLogOf
import io.github.alexandrepiveteau.echo.core.log.mutableHistoryOf
import io.github.alexandrepiveteau.echo.events.EventScope
import io.github.alexandrepiveteau.echo.projections.OneWayMutableProjection
import io.github.alexandrepiveteau.echo.projections.OneWayProjection
import io.github.alexandrepiveteau.echo.projections.TwoWayMutableProjection
import io.github.alexandrepiveteau.echo.projections.TwoWayProjection
import io.github.alexandrepiveteau.echo.protocol.ExchangeImpl
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import io.github.alexandrepiveteau.echo.protocol.MutableSiteImpl
import io.github.alexandrepiveteau.echo.protocol.SiteImpl
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * An interface describing a [Site] in the distributed system. When collected, it will emit the
 * latest aggregated model.
 *
 * @param M the type of the underlying aggregated model for this [Site].
 */
public interface Site<out M> : Exchange<Inc, Out> {

  /** Returns the current value. */
  public val value: Flow<M>
}

/**
 * A mutable version of [Site], which allows the insertion of the events [T] through its [event]
 * method. Each [MutableSite] is associated with a globally unique [SiteIdentifier], which will be
 * used when yielding events.
 *
 * @param T the type of the events managed by this [Site].
 * @param M the type of the underlying aggregated model for this [Site].
 */
public interface MutableSite<in T, out M> : Site<M> {

  /** The globally unique [SiteIdentifier] for this [Site]. */
  public val identifier: SiteIdentifier

  /**
   * Creates some new events, that are generated in the [EventScope]. This function returns an
   * arbitrary user-provided value once the events have been successfully added to the underlying
   * [MutableSite].
   *
   * The whole [block] is guaranteed to be evaluated atomically on the event log.
   *
   * @param R the return value of the [event] call.
   */
  public suspend fun <R> event(block: EventScope<T>.(M) -> R): R
}

/**
 * An enumeration defining the strategy that will implement the replication protocol. A
 * [SyncStrategy] defines the behavior for both sides of the replication protocol.
 */
public enum class SyncStrategy {

  /**
   * A [SyncStrategy] that stops sync between two sites only whenever the other side closes the
   * communication channel. You'll typically want to use this for background sync.
   */
  Continuous,

  /**
   * A [SyncStrategy] that syncs between two sites, and stops when all the advertised events (before
   * the [I.Ready] message) have been sent or received. You'll typically want to use this for
   * foreground, user-initiated one-shot sync.
   */
  Once,
}

/**
 * Creates a new [Exchange] with a backing event log.
 *
 * @param events some initial events to populate the event log.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 */
public fun exchange(
    vararg events: Pair<EventIdentifier, ByteArray>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
): Exchange<Inc, Out> =
    exchange(
        log = mutableEventLogOf(*events),
        strategy = strategy,
    )

/**
 * Creates a new [Exchange] with a backing event log.
 *
 * @param log the [MutableEventLog] backing the exchange.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 */
public fun exchange(
    log: MutableEventLog,
    strategy: SyncStrategy = SyncStrategy.Continuous,
): Exchange<Inc, Out> =
    ExchangeImpl(
        log = log,
        strategy = strategy,
    )

/**
 * Creates a new [Site] with a backing history.
 *
 * @param history the [MutableHistory] backing the site.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 */
public inline fun <M, reified T> site(
    history: MutableHistory<M>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
): Site<M> =
    site(
        history = history,
        strategy = strategy,
        transform = { it },
    )

/**
 * Creates a new [Site] with a backing history.
 *
 * @param events some initial events to populate the history.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 *
 * @param T the type of the events managed by this [Site].
 */
public inline fun <reified T> site(
    vararg events: Pair<EventIdentifier, T>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
): Site<Unit> =
    site(
        initial = Unit,
        projection = UnitProjection,
        events = events,
        strategy = strategy,
    )

/**
 * Creates a new [Site] for the provided [SiteIdentifier], with a backing log. Additionally, this
 * overload takes a [OneWayProjection] and lets you specify a projection to apply to the data.
 *
 * @param initial the initial value for the projection aggregate.
 * @param projection the [OneWayProjection] for this [Site].
 * @param events some initial events to populate the history.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 */
public inline fun <M, reified T> site(
    initial: M,
    projection: OneWayProjection<M, T>,
    vararg events: Pair<EventIdentifier, T>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
): Site<M> =
    site(
        history =
            mutableHistoryOf(
                initial = initial,
                projection =
                    OneWayMutableProjection(
                        projection = projection,
                        eventSerializer = serializer(),
                        format = DefaultBinaryFormat),
                events = events.mapToBinary(serializer(), DefaultBinaryFormat),
            ),
        strategy = strategy,
        transform = { it },
    )

/**
 * Creates a new [Site] for the provided [SiteIdentifier], with a backing log. Additionally, this
 * overload takes a [TwoWayProjection] and lets you specify a projection to apply to the data.
 *
 * @param initial the initial value for the projection aggregate.
 * @param projection the [TwoWayProjection] for this [Site].
 * @param events some initial events to populate the history.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 * @param C the type of the changes generated by this [Site].
 */
public inline fun <M, reified T, reified C> site(
    initial: M,
    projection: TwoWayProjection<M, T, C>,
    vararg events: Pair<EventIdentifier, T>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
): Site<M> =
    site(
        history =
            mutableHistoryOf(
                initial = initial,
                projection =
                    TwoWayMutableProjection(
                        projection = projection,
                        changeSerializer = serializer(),
                        eventSerializer = serializer(),
                        format = DefaultBinaryFormat,
                    ),
                events = events.mapToBinary(serializer(), DefaultBinaryFormat),
            ),
        strategy = strategy,
        transform = { it },
    )

/**
 * Creates a new [Site] for the provided [SiteIdentifier], with a backing log.
 *
 * @param history the [MutableHistory] for this [Site].
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 * @param transform a function mapping the backing model from type [R] to type [M].
 *
 * @param M the type of the model for this [Site].
 * @param R the type of the backing model for this [Site].
 */
public fun <M, R> site(
    history: MutableHistory<R>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
    transform: (R) -> M,
): Site<M> =
    SiteImpl(
        history = history,
        strategy = strategy,
        transform = transform,
    )

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing history. The
 * current model value of the site will always be [Unit], since it does not perform aggregations.
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param events some initial events to populate the history.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 *
 * @param T the type of the events managed by this [Site].
 */
public inline fun <reified T> mutableSite(
    identifier: SiteIdentifier,
    vararg events: Pair<EventIdentifier, T>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
): MutableSite<T, Unit> =
    mutableSite(
        identifier = identifier,
        initial = Unit,
        projection = UnitProjection,
        events = events,
        strategy = strategy,
    )

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing log. Additionally,
 * this overload takes a [OneWayProjection] and lets you specify a projection to apply to the data,
 * to have custom [MutableSite.event] arguments.
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param history the [MutableHistory] backing the site.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 */
public inline fun <M, reified T> mutableSite(
    identifier: SiteIdentifier,
    history: MutableHistory<M>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
): MutableSite<T, M> =
    mutableSite(
        identifier = identifier,
        history = history,
        strategy = strategy,
        eventSerializer = serializer(),
        format = DefaultBinaryFormat,
        transform = { it },
    )

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing log. Additionally,
 * this overload takes a [OneWayProjection] and lets you specify a projection to apply to the data,
 * to have custom [MutableSite.event] arguments.
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param initial the initial value for the projection aggregate.
 * @param projection the [OneWayProjection] for this [Site].
 * @param events some initial events to populate the history.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 */
public inline fun <M, reified T> mutableSite(
    identifier: SiteIdentifier,
    initial: M,
    projection: OneWayProjection<M, T>,
    vararg events: Pair<EventIdentifier, T>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
): MutableSite<T, M> =
    mutableSite(
        identifier = identifier,
        initial = initial,
        projection = projection,
        events = events,
        strategy = strategy,
        transform = { it },
    )

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing log. Additionally,
 * this overload takes a [TwoWayProjection] and lets you specify a projection to apply to the data,
 * to have custom [MutableSite.event] arguments.
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param initial the initial value for the projection aggregate.
 * @param projection the [TwoWayProjection] for this [Site].
 * @param events some initial events to populate the history.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 * @param C the type of the changes generated by this [Site].
 */
public inline fun <M, reified T, reified C> mutableSite(
    identifier: SiteIdentifier,
    initial: M,
    projection: TwoWayProjection<M, T, C>,
    vararg events: Pair<EventIdentifier, T>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
): MutableSite<T, M> =
    mutableSite(
        identifier = identifier,
        initial = initial,
        projection = projection,
        events = events,
        strategy = strategy,
        transform = { it },
    )

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing mutable history.
 * Additionally, this overload takes a [OneWayProjection] and lets you specify a projection to apply
 * to the data, to have custom [MutableSite.event] arguments.
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param history the [MutableHistory] backing the site.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 * @param transform a function mapping the backing model from type [R] to type [M].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 * @param R the type of the backing model for this [Site].
 */
public inline fun <M, reified T, R> mutableSite(
    identifier: SiteIdentifier,
    history: MutableHistory<R>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
    noinline transform: (R) -> M,
): MutableSite<T, M> =
    mutableSite(
        identifier = identifier,
        history = history,
        eventSerializer = serializer(),
        format = DefaultBinaryFormat,
        strategy = strategy,
        transform = transform,
    )

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing mutable history.
 * Additionally, this overload takes a [OneWayProjection] and lets you specify a projection to apply
 * to the data, to have custom [MutableSite.event] arguments.
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param initial the initial value for the projection aggregate.
 * @param projection the [OneWayProjection] for this [Site].
 * @param events some initial events to populate the history.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 * @param transform a function mapping the backing model from type [R] to type [M].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 * @param R the type of the backing model for this [Site].
 */
public inline fun <M, reified T, R> mutableSite(
    identifier: SiteIdentifier,
    initial: R,
    projection: OneWayProjection<R, T>,
    vararg events: Pair<EventIdentifier, T>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
    noinline transform: (R) -> M,
): MutableSite<T, M> =
    mutableSite(
        identifier = identifier,
        history =
            mutableHistoryOf(
                initial = initial,
                projection =
                    OneWayMutableProjection(
                        projection = projection,
                        eventSerializer = serializer(),
                        format = DefaultBinaryFormat,
                    ),
                events = events.mapToBinary(serializer(), DefaultBinaryFormat),
            ),
        strategy = strategy,
        transform = transform,
    )

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing mutable history.
 * Additionally, this overload takes a [TwoWayProjection] and lets you specify a projection to apply
 * to the data, to have custom [MutableSite.event] arguments.
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param initial the initial value for the projection aggregate.
 * @param projection the [TwoWayProjection] for this [Site].
 * @param events some initial events to populate the history.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 * @param transform a function mapping the backing model from type [R] to type [M].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 * @param C the type of the changes generated by this [Site].
 * @param R the type of the backing model for this [Site].
 */
public inline fun <M, reified T, reified C, R> mutableSite(
    identifier: SiteIdentifier,
    initial: R,
    projection: TwoWayProjection<R, T, C>,
    vararg events: Pair<EventIdentifier, T>,
    strategy: SyncStrategy = SyncStrategy.Continuous,
    noinline transform: (R) -> M,
): MutableSite<T, M> =
    mutableSite(
        identifier = identifier,
        history =
            mutableHistoryOf(
                initial = initial,
                projection =
                    TwoWayMutableProjection(
                        projection = projection,
                        changeSerializer = serializer(),
                        eventSerializer = serializer(),
                        format = DefaultBinaryFormat,
                    ),
                events = events.mapToBinary(serializer(), DefaultBinaryFormat),
            ),
        eventSerializer = serializer(),
        format = DefaultBinaryFormat,
        strategy = strategy,
        transform = transform,
    )

/**
 * Creates a new [MutableSite] for the provided [SiteIdentifier], with a backing mutable history.
 *
 * @param identifier the globally unique identifier for this [Site].
 * @param history the underlying [MutableHistory].
 * @param eventSerializer the [KSerializer] for the events.
 * @param format the binary format to use.
 * @param strategy the [SyncStrategy] that's applied. Defaults to [SyncStrategy.Continuous].
 * @param transform a function mapping the backing model from type [R] to type [M].
 *
 * @param M the type of the model for this [Site].
 * @param T the type of the events managed by this [Site].
 * @param R the type of the backing model for this [Site].
 */
public fun <M, T, R> mutableSite(
    identifier: SiteIdentifier,
    history: MutableHistory<R>,
    eventSerializer: KSerializer<T>,
    format: BinaryFormat = DefaultBinaryFormat,
    strategy: SyncStrategy = SyncStrategy.Continuous,
    transform: (R) -> M,
): MutableSite<T, M> =
    MutableSiteImpl(
        identifier = identifier,
        serializer = eventSerializer,
        history = history,
        format = format,
        strategy = strategy,
        transform = transform,
    )

// SITE BUILDERS

@PublishedApi
internal object UnitProjection : OneWayProjection<Unit, Any?> {
  override fun forward(
      model: Unit,
      identifier: EventIdentifier,
      event: Any?,
  ) = model
}

/**
 * Transforms a vararg of [Pair] of [EventIdentifier] to [T] to a map an [Array] of [Pair] of
 * [EventIdentifier] to [ByteArray].
 *
 * @param serializer the [KSerializer] that is used for serialization.
 * @param format the [BinaryFormat] used to convert to a binary format.
 */
@PublishedApi
internal fun <T> Array<out Pair<EventIdentifier, T>>.mapToBinary(
    serializer: KSerializer<T>,
    format: BinaryFormat,
): Array<Pair<EventIdentifier, ByteArray>> =
    asSequence()
        .map { (id, body) -> id to format.encodeToByteArray(serializer, body) }
        .toList()
        .toTypedArray()
