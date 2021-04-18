import kotlinx.browser.document
import react.RProps
import react.child
import react.dom.h1
import react.dom.p
import react.dom.render
import react.functionalComponent

val App =
    functionalComponent<RProps> {
      h1 { +"This is my first title" }
      p { +"And this is my first paragraph." }
    }

fun main() {
  render(document.getElementById("root")) { child(App) }
}
