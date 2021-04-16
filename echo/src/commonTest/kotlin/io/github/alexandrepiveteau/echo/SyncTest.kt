package io.github.alexandrepiveteau.echo

import kotlin.test.Test

class SyncTest {

  @Test
  fun `NoOp simple sync eventually terminates`() = suspendTest {
    val alice = NoOpExchange
    val bob = NoOpExchange

    sync(alice, bob)
  }

  @Test
  fun `NoOp chain sync eventually terminates`() = suspendTest {
    val head = NoOpExchange
    val tail = Array(10) { NoOpExchange }

    sync(head, *tail)
  }
}

object NoOpExchange : Exchange<Nothing, Nothing> {
  override fun incoming() = link<Nothing, Nothing> {}
  override fun outgoing() = link<Nothing, Nothing> {}
}
