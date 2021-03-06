@file:OptIn(ExperimentalCoroutinesApi::class)

package markdown.echo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ExchangeTest {

  @Test
  fun `Empty channelExchange properly terminates`() = runBlocking {
    val exchange = channelExchange<Unit, Unit> {}
    assertEquals(emptyList(), exchange.talk(emptyFlow()).toList())
  }

  @Test
  fun `channelExchange emits proper messages then terminates`() = runBlocking {
    val exchange =
        channelExchange<Int, Int> {
          send(1)
          send(2)
          send(3)
        }
    val result = exchange.talk(emptyFlow()).toList()
    assertEquals(listOf(1, 2, 3), result)
  }

  @Test
  fun `reversing channelExchange works properly`() = runBlocking {
    val exchange =
        channelExchange<Int, Int> { incoming ->
          val elements = incoming.toList().asReversed()
          elements.forEach { send(it) }
        }
    val result = exchange.talk(flowOf(1, 2, 3)).toList()
    assertEquals(listOf(3, 2, 1), result)
  }

  @Test
  fun `channelExchange waits inner coroutines`() = runBlocking {
    val exchange =
        channelExchange<Int, Int> {
          launch {
            delay(100)
            send(123)
          }
        }
    val result = exchange.talk(emptyFlow()).single()
    assertEquals(123, result)
  }
}
