package markdown.party.demo.string

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import markdown.echo.causal.EventIdentifier
import markdown.echo.causal.SiteIdentifier
import markdown.echo.events.EventScope
import markdown.echo.mutableSite
import markdown.echo.sync
import markdown.party.demo.string.StringOperation.InsertAfter

class StringTest {

  /**
   * Adds some text at the beginning of the current content.
   *
   * @param text the body of the text that should be written.
   */
  private suspend fun EventScope<StringOperation>.appendStart(text: String) {
    var after: EventIdentifier? = null
    for (char in text) {
      after = yield(InsertAfter(char, after))
    }
  }

  /**
   * Adds some text at the end of the current content.
   *
   * @param text the body of the text that should be written.
   */
  private suspend fun EventScope<StringOperation>.appendEnd(model: StringModel, text: String) {
    var after = model.lastOrNull()?.first
    for (char in text) {
      after = yield(InsertAfter(char, after))
    }
  }

  /** Removes all the text within a certain range. */
  private suspend fun EventScope<StringOperation>.deleteRange(
      model: StringModel,
      from: Int,
      until: Int,
  ) {
    val indices = model.asSequence().filter { it.second != null }.map { it.first }.toList()
    for (i in from until until) {
      yield(StringOperation.Remove(indices[i]))
    }
  }

  // TESTS

  @Test
  fun `one site is able to write a simple text`(): Unit = runBlocking {
    val alice =
        mutableSite(
            identifier = SiteIdentifier.random(),
            initial = emptyList(),
            projection = StringProjection(),
        )
    val message = "Hello world !"

    alice.event { appendStart(message) }

    // Ensure proper termination.
    val actual = alice.value.map { it.asString() }.first { it == message }
    assertEquals(message, actual)
  }

  @Test
  fun `one site can write then delete some text`(): Unit = runBlocking {
    val alice =
        mutableSite(
            identifier = SiteIdentifier(0),
            initial = emptyList(),
            projection = StringProjection(),
        )
    alice.event { appendStart("Hello World !") }
    alice.event { deleteRange(it, 0, 6) }
    alice.value.map { it.asString() }.filter { it == "World !" }.first()
  }

  @Test
  fun `two sites are able to edit each other's text`(): Unit = runBlocking {
    val alice =
        mutableSite(
            identifier = SiteIdentifier(0),
            initial = emptyList(),
            projection = StringProjection(),
        )
    val bob =
        mutableSite(
            identifier = SiteIdentifier(1),
            initial = emptyList(),
            projection = StringProjection(),
        )

    val message = "Hello world, this is a test !"

    alice.event { appendStart(message) }

    withTimeoutOrNull(1000) { sync(alice, bob) }

    alice.event { model -> appendEnd(model, " Hurray !") }
    bob.event { model -> deleteRange(model, 6, 6 + "world".length) }

    withTimeoutOrNull(1000) { sync(alice, bob) }

    val expected = "Hello , this is a test ! Hurray !"

    bob.value.map { it.asString() }.first { it == expected }
    alice.value.map { it.asString() }.first { it == expected }
  }
}
