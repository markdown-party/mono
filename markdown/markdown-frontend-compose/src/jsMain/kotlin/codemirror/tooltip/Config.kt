package codemirror.tooltip

import codemirror.view.EditorView

/** Configures a [Tooltip] using the [block] builder. */
fun Tooltip(
    pos: Int,
    create: (view: EditorView) -> TooltipView,
    block: Tooltip.() -> Unit,
): Tooltip {
  val configDynamic = js("{}")
  configDynamic.pos = pos
  configDynamic.create = create
  val config = configDynamic.unsafeCast<Tooltip>()
  block(config)
  return config
}

/** Configures a [TooltipView] using the [block] builder. */
fun TooltipView(block: TooltipView.() -> Unit): TooltipView {
  val config = js("{}").unsafeCast<TooltipView>()
  block(config)
  return config
}
