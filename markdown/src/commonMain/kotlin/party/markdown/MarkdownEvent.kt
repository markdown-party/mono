package party.markdown

import io.github.alexandrepiveteau.echo.Coder as EchoCoder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class MarkdownEvent {
  Increment,
  Decrement;

  companion object Coder : EchoCoder<MarkdownEvent, String> {
    override fun encode(it: MarkdownEvent) = Json.encodeToString(serializer(), it)
    override fun decode(it: String) = Json.decodeFromString(serializer(), it)
  }
}
