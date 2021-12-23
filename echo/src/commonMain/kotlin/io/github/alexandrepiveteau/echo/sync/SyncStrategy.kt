package io.github.alexandrepiveteau.echo.sync

import io.github.alexandrepiveteau.echo.protocol.*
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import kotlin.jvm.JvmField

/**
 * An interface defining the strategy that will implement the replication protocol. A [SyncStrategy]
 * defines the behavior for both sides of the replication protocol.
 *
 * @param I the type of the incoming messages
 * @param O the type of the outgoing messages
 */
sealed interface SyncStrategy<I, O> {

  /**
   * Describes the outgoing side of the [SyncStrategy], which will receive some [I] and send some
   * [O] to the other side.
   */
  suspend fun ExchangeScope<I, O>.outgoing()

  /**
   * Describes the incoming side of the [SyncStrategy], which will receive some [O] and send some
   * [I] to the other side.
   */
  suspend fun ExchangeScope<O, I>.incoming()

  companion object {

    /**
     * A [SyncStrategy] that stops sync between two sites only whenever the other side closes the
     * communication channel. You'll typically want to use this for background sync.
     */
    @JvmField val Continuous: SyncStrategy<Inc, Out> = SyncContinuous

    /**
     * A [SyncStrategy] that syncs between two sites, and stops when all the advertised events
     * (before the [I.Ready] message) have been sent or received. You'll typically want to use this
     * for foreground, user-initiated one-shot sync.
     */
    @JvmField val Once: SyncStrategy<Inc, Out> = SyncOnce
  }
}

private object SyncContinuous : SyncStrategy<Inc, Out> {
  override suspend fun ExchangeScope<Inc, Out>.outgoing() = runCatchingTermination {
    awaitEvents(
        advertisements = awaitAdvertisements(),
        stopAfterAdvertised = false,
    )
  }

  override suspend fun ExchangeScope<Out, Inc>.incoming() = runCatchingTermination {
    outgoingSending(
        advertised = outgoingAdvertiseAll(),
        stopAfterAdvertised = false,
    )
  }
}

private object SyncOnce : SyncStrategy<Inc, Out> {
  override suspend fun ExchangeScope<Inc, Out>.outgoing() = runCatchingTermination {
    awaitEvents(
        advertisements = awaitAdvertisements(),
        stopAfterAdvertised = true,
    )
  }
  override suspend fun ExchangeScope<Out, Inc>.incoming() = runCatchingTermination {
    outgoingSending(
        advertised = outgoingAdvertiseAll(),
        stopAfterAdvertised = true,
    )
  }
}
