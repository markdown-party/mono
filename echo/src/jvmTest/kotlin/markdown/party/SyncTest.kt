package markdown.party

import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import markdown.echo.Exchange
import markdown.echo.link
import markdown.echo.sync

class SyncTest {

  @Test
  fun `NoOp simple sync eventually terminates`() = runBlocking {
    val alice = NoOpExchange
    val bob = NoOpExchange

    sync(alice, bob)
  }

  @Test
  fun `NoOp chain sync eventually terminates`() = runBlocking {
    val head = NoOpExchange
    val tail = Array(10) { NoOpExchange }

    sync(head, *tail)
  }
}

object NoOpExchange : Exchange<Nothing, Nothing> {
  override fun incoming() = link<Nothing, Nothing> {}
  override fun outgoing() = link<Nothing, Nothing> {}
}
