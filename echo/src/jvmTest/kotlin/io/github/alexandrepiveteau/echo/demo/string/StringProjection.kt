package io.github.alexandrepiveteau.echo.demo.string

import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.logs.EventLog
import io.github.alexandrepiveteau.echo.projections.OneWayProjection

typealias StringModel = List<Pair<EventIdentifier, Char?>>

fun StringModel.asString() =
    asSequence()
        .map { it.second }
        .filterNotNull()
        .toList()
        .toTypedArray()
        .toCharArray()
        .concatToString()

class StringProjection : OneWayProjection<StringModel, EventLog.Entry<StringOperation>> {

  override fun forward(body: EventLog.Entry<StringOperation>, model: StringModel): StringModel {
    return when (val op = body.body) {
      is StringOperation.InsertAfter -> {
        model.toMutableList().apply {
          val index = indexOfFirst { it.first == op.after }
          add(index + 1, body.identifier to op.character)
        }
      }
      is StringOperation.Remove -> {
        model.map { (id, letter) -> if (id != op.identifier) id to letter else id to null }
      }
    }
  }
}
