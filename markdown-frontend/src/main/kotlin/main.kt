import kotlinx.browser.document
import party.markdown.demo.sockets
import react.dom.render

fun main() {
  render(document.getElementById("root")) { sockets() }
}
