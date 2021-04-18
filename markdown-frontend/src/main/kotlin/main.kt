import io.github.alexandrepiveteau.echo.causal.SiteIdentifier.Companion.random
import io.github.alexandrepiveteau.echo.mutableSite
import kotlinx.browser.document
import kotlinx.coroutines.launch
import kotlinx.html.js.onClickFunction
import party.markdown.MarkdownEvent.Decrement
import party.markdown.MarkdownEvent.Increment
import party.markdown.MarkdownProjection
import party.markdown.react.useCoroutineScope
import party.markdown.react.useFlow
import react.RProps
import react.child
import react.dom.button
import react.dom.h1
import react.dom.p
import react.dom.render
import react.functionalComponent
import react.useEffect

private val State =
    mutableSite(
        identifier = random(),
        initial = 0,
        projection = MarkdownProjection,
    )

private val App =
    functionalComponent<RProps> {
      val state = useFlow(0, State.value)
      val scope = useCoroutineScope()

      useEffect(listOf(state)) { document.title = "Clicked $state times" }

      h1 { +"Counter" }
      p { +"Current value is $state" }
      button {
        attrs.onClickFunction = { scope.launch { State.event { yield(Increment) } } }
        +"Increment"
      }
      button {
        attrs.onClickFunction = { scope.launch { State.event { yield(Decrement) } } }
        +"Decrement"
      }
    }

fun main() {
  render(document.getElementById("root")) { child(App) }
}
