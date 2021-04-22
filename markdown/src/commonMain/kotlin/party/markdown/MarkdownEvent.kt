package party.markdown

import kotlinx.serialization.Serializable

@Serializable
enum class MarkdownEvent {
  Increment,
  Decrement,
}
