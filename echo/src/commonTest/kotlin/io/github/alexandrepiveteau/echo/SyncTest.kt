package io.github.alexandrepiveteau.echo

import kotlin.test.Test

class SyncTest {

  @Test
  fun noOpSync_Terminates() = suspendTest {
    val alice = NoOpExchange
    val bob = NoOpExchange

    sync(alice, bob)
  }

  @Test
  fun noOpChainSync_Terminates() = suspendTest {
    val head = NoOpExchange
    val tail = Array(10) { NoOpExchange }

    sync(head, *tail)
  }
}

object NoOpExchange : Exchange<Nothing, Nothing> {
  override fun incoming() = link<Nothing, Nothing> {}
  override fun outgoing() = link<Nothing, Nothing> {}
}
