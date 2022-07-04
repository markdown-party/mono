package party.markdown.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div

private const val DividerColor = "bg-gray-500"

/** Splits a `flex-row` component with a divider spanning vertically. */
@Composable
fun DividerHorizontal() {
  Div(attrs = { classes(DividerColor, "w-1") })
}

/** Splits a `flex-col` component with an divider spanning horizontally. */
@Composable
fun DividerVertical() {
  Div(attrs = { classes(DividerColor, "h-1") })
}
