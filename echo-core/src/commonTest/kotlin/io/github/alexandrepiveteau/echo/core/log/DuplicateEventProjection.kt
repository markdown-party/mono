package io.github.alexandrepiveteau.echo.core.log

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import kotlin.test.assertContentEquals

object DuplicateEventProjection : MutableProjection<Unit> {

  override fun ChangeScope.forward(
      model: Unit,
      identifier: EventIdentifier,
      data: ByteArray,
      from: Int,
      until: Int
  ) {
    push(data, from, until)
  }

  override fun backward(
      model: Unit,
      identifier: EventIdentifier,
      data: ByteArray,
      from: Int,
      until: Int,
      changeData: ByteArray,
      changeFrom: Int,
      changeUntil: Int
  ) {
    val a = data.copyOfRange(from, until)
    val b = changeData.copyOfRange(changeFrom, changeUntil)
    assertContentEquals(a, b)
  }
}
