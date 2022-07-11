package io.github.alexandrepiveteau.echo

import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest

class SyncTest {

  @Test
  fun noOpSync_Terminates() = runTest {
    val alice = NoOpExchange
    val bob = NoOpExchange

    sync(alice, bob)
  }

  @Test
  fun noOpChainSync_Terminates() = runTest {
    val head = NoOpExchange
    val tail = Array(10) { NoOpExchange }

    sync(head, *tail)
  }

  @Test
  fun singleExchange_syncAll_terminates() = runTest {
    val exchange = SuspendingExchange
    syncAll(exchange)
  }
}

private fun suspendingFlow(): Flow<Nothing> = flow { suspendCancellableCoroutine {} }

object NoOpExchange : Exchange<Nothing, Nothing> {
  override fun receive(incoming: Flow<Nothing>) = emptyFlow<Nothing>()
  override fun send(incoming: Flow<Nothing>) = emptyFlow<Nothing>()
}

object SuspendingExchange : Exchange<Nothing, Nothing> {
  override fun receive(incoming: Flow<Nothing>) = suspendingFlow()
  override fun send(incoming: Flow<Nothing>) = suspendingFlow()
}
