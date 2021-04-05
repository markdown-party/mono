package markdown.party.demo.string

import markdown.echo.causal.EventIdentifier
import markdown.echo.logs.EventValue
import markdown.echo.projections.OneWayProjection

typealias StringModel = List<Pair<EventIdentifier, Char?>>

fun StringModel.asString() =
    asSequence()
        .map { it.second }
        .filterNotNull()
        .toList()
        .toTypedArray()
        .toCharArray()
        .concatToString()

class StringProjection : OneWayProjection<StringModel, EventValue<StringOperation>> {

  override fun forward(
      body: EventValue<StringOperation>,
      model: StringModel
  ): StringModel {
    val (_, op) = body
    return when (op) {
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
