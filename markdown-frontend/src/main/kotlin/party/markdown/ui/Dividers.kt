package party.markdown.ui

import react.RBuilder
import react.ReactElement
import react.dom.div

private const val DividerColor = "bg-gray-500"

/** Splits a `flex-row` component with a divider spanning vertically. */
fun RBuilder.dividerHorizontal(): ReactElement {
  return div("$DividerColor w-1") {}
}

/** Splits a `flex-col` component with an divider spanning horizontally. */
fun RBuilder.dividerVertical(): ReactElement {
  return div("$DividerColor h-1") {}
}
