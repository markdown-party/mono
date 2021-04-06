package io.github.alexandrepiveteau.markdown

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class CounterEvent(val plus: Int)

val Coder =
    object : io.github.alexandrepiveteau.echo.Coder<CounterEvent, String> {
      override fun encode(it: CounterEvent) = Json.encodeToString(CounterEvent.serializer(), it)
      override fun decode(it: String) = Json.decodeFromString(CounterEvent.serializer(), it)
    }
