@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.alexandrepiveteau.echo

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

class LinkTest {

  @Test
  fun `Empty channelLink properly terminates`() = suspendTest {
    val exchange = channelLink<Unit, Unit> {}
    assertEquals(emptyList(), exchange.talk(emptyFlow()).toList())
  }

  @Test
  fun `channelLink emits proper messages then terminates`() = suspendTest {
    val exchange =
        channelLink<Int, Int> {
          send(1)
          send(2)
          send(3)
        }
    val result = exchange.talk(emptyFlow()).toList()
    assertEquals(listOf(1, 2, 3), result)
  }

  @Test
  fun `reversing channelLink works properly`() = suspendTest {
    val exchange =
        channelLink<Int, Int> { incoming ->
          val elements = incoming.toList().asReversed()
          elements.forEach { send(it) }
        }
    val result = exchange.talk(flowOf(1, 2, 3)).toList()
    assertEquals(listOf(3, 2, 1), result)
  }

  @Test
  fun `channelLink waits inner coroutines`() = suspendTest {
    val exchange =
        channelLink<Int, Int> {
          launch {
            delay(100)
            send(123)
          }
        }
    val result = exchange.talk(emptyFlow()).single()
    assertEquals(123, result)
  }
}