package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber
import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import kotlin.test.Test
import kotlin.test.assertContentEquals

class MutableHistoryInsertTest {

  object Projection : MutableProjection<List<Byte>> {

    override fun ChangeScope.forward(
        model: List<Byte>,
        identifier: EventIdentifier,
        data: ByteArray,
        from: Int,
        until: Int
    ): List<Byte> {
      val bytes = data.copyOfRange(from, until)
      push(bytes)
      return model + bytes.asList()
    }

    override fun backward(
        model: List<Byte>,
        identifier: EventIdentifier,
        data: ByteArray,
        from: Int,
        until: Int,
        changeData: ByteArray,
        changeFrom: Int,
        changeUntil: Int
    ): List<Byte> {
      assertContentEquals(
          data.copyOfRange(from, until),
          changeData.copyOfRange(changeFrom, changeUntil),
      )
      return model.dropLast(until - from)
    }
  }

  @Test
  fun insert_threeTimes() {
    val alice = 1U.toSiteIdentifier()
    val bob = 2U.toSiteIdentifier()

    val history = mutableHistoryOf(emptyList(), Projection)
    history.insert(SequenceNumber.Min + 0u, alice, byteArrayOf(1, 1, 1))
    history.insert(SequenceNumber.Min + 0u, bob, byteArrayOf(2, 2))
    history.insert(SequenceNumber.Min + 1u, alice, byteArrayOf(3, 3, 3))

    assertContentEquals(listOf(1, 1, 1, 2, 2, 3, 3, 3), history.current)
  }

  @Test
  fun insert_threeTimes_reorder() {
    val alice = 1U.toSiteIdentifier()
    val bob = 2U.toSiteIdentifier()

    val history = mutableHistoryOf(emptyList(), Projection)
    history.insert(SequenceNumber.Min + 0u, alice, byteArrayOf(1, 1, 1))
    history.insert(SequenceNumber.Min + 1u, bob, byteArrayOf(2, 2))
    history.insert(SequenceNumber.Min + 1u, alice, byteArrayOf(3, 3, 3))

    assertContentEquals(listOf(1, 1, 1, 3, 3, 3, 2, 2), history.current)
  }

  @Test
  fun merge_reorder() {
    val alice = 1U.toSiteIdentifier()
    val bob = 2U.toSiteIdentifier()

    val history = mutableHistoryOf(emptyList(), Projection)
    history.insert(SequenceNumber.Min + 0u, alice, byteArrayOf(1, 1, 1))

    val history2 = mutableHistoryOf(emptyList(), Projection)
    history2.insert(SequenceNumber.Min + 1u, bob, byteArrayOf(2, 2))
    history2.insert(SequenceNumber.Min + 1u, alice, byteArrayOf(3, 3, 3))
    history.merge(from = history2)

    assertContentEquals(listOf(1, 1, 1, 3, 3, 3, 2, 2), history.current)
  }
}
