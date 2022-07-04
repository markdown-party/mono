package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.buffer.MutableByteGapBuffer
import io.github.alexandrepiveteau.echo.core.buffer.copyOfRange
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import kotlin.test.assertContentEquals

object DuplicateEventProjection : MutableProjection<Unit> {

  override fun ChangeScope.forward(
      model: Unit,
      identifier: EventIdentifier,
      data: MutableByteGapBuffer,
      from: Int,
      until: Int
  ) {
    push(data.copyOfRange(from, until), 0, until - from)
  }

  override fun backward(
      model: Unit,
      identifier: EventIdentifier,
      data: MutableByteGapBuffer,
      from: Int,
      until: Int,
      changeData: MutableByteGapBuffer,
      changeFrom: Int,
      changeUntil: Int
  ) {
    val a = data.copyOfRange(from, until)
    val b = changeData.copyOfRange(changeFrom, changeUntil)
    assertContentEquals(a, b)
  }
}
