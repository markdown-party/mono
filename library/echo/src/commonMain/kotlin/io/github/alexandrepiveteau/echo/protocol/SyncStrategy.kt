package io.github.alexandrepiveteau.echo.protocol

import io.github.alexandrepiveteau.echo.SyncStrategy
import io.github.alexandrepiveteau.echo.protocol.*
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out

/** Executes the [SyncStrategy] in the given [ExchangeScope]. */
internal suspend fun SyncStrategy.outgoing(
    scope: ExchangeScope<Inc, Out>,
) = runCatchingTermination {
  with(scope) {
    awaitEvents(
        advertisements = awaitAdvertisements(),
        stopAfterAdvertised = this@outgoing == SyncStrategy.Once,
    )
  }
}

/** Executes the [SyncStrategy] in the given [ExchangeScope]. */
internal suspend fun SyncStrategy.incoming(
    scope: ExchangeScope<Out, Inc>,
) = runCatchingTermination {
  with(scope) {
    outgoingSending(
        advertised = outgoingAdvertiseAll(),
        stopAfterAdvertised = this@incoming == SyncStrategy.Once,
    )
  }
}
