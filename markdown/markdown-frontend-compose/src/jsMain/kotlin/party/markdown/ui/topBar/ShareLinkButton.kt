package party.markdown.ui.topBar

import androidx.compose.runtime.*
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun ShareLinkButton(
    link: String,
) {
  var copied by remember { mutableStateOf(false) }
  LaunchedEffect(copied) {
    if (copied) {
      delay(2000)
      copied = false
    }
  }

  val (text, icon) =
      when (copied) {
        true -> "Copied to clipboard" to "/icons/share-done.svg"
        false -> "Share link" to "/icons/share-copy.svg"
      }

  Button(
      onClick = {
        copied = true
        window.navigator.clipboard.writeText(link)
      },
  ) {
    Img(src = icon)
    Span { Text(text) }
  }
}
