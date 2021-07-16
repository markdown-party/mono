package io.github.alexandrepiveteau.echo.sync

import io.github.alexandrepiveteau.echo.protocol.*
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as I
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as O

/**
 * An interface defining the strategy that will implement the replication protocol. A [SyncStrategy]
 * defines the behavior for both sides of the replication protocol.
 */
sealed interface SyncStrategy {

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
    val Continuous: SyncStrategy = SyncContinuous

    /**
     * A [SyncStrategy] that syncs between two sites, and stops when all the advertised events
     * (before the [I.Ready] message) have been sent or received. You'll typically want to use this
     * for foreground, user-initiated one-shot sync.
     */
    val Once: SyncStrategy = SyncOnce
  }
}

private object SyncContinuous : SyncStrategy {
  override suspend fun ExchangeScope<I, O>.outgoing() = runCatchingTermination {
    awaitEvents(
        advertisements = awaitAdvertisements(),
        stopAfterAdvertised = false,
    )
  }

  override suspend fun ExchangeScope<O, I>.incoming() = runCatchingTermination {
    outgoingSending(
        advertised = outgoingAdvertiseAll(),
        stopAfterAdvertised = false,
    )
  }
}

private object SyncOnce : SyncStrategy {
  override suspend fun ExchangeScope<I, O>.outgoing() = runCatchingTermination {
    awaitEvents(
        advertisements = awaitAdvertisements(),
        stopAfterAdvertised = true,
    )
  }
  override suspend fun ExchangeScope<O, I>.incoming() = runCatchingTermination {
    outgoingSending(
        advertised = outgoingAdvertiseAll(),
        stopAfterAdvertised = true,
    )
  }
}
