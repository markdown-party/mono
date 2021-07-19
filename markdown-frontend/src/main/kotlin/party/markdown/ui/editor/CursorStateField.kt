package party.markdown.ui.editor

import codemirror.state.Annotation
import codemirror.state.EditorState
import codemirror.state.StateField
import codemirror.state.StateFieldConfig
import codemirror.tooltip.Tooltip
import codemirror.tooltip.TooltipView
import codemirror.tooltip.showTooltip
import codemirror.view.EditorView
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifierArray
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import party.markdown.cursors.Cursors

val CursorAnnotation = Annotation.define<Set<Cursors.Cursor>>()

data class CursorsState(
    val actor: SiteIdentifier,
    val cursors: Set<Cursors.Cursor>,
)

fun cursorsStateField(actor: SiteIdentifier): StateField<CursorsState> {
  return StateField.define(
      StateFieldConfig(
          create = { CursorsState(actor, emptySet()) },
          update = { tooltips, tr ->
            if (tr.annotation(CursorAnnotation) != undefined) {
              tooltips.copy(cursors = tr.annotation(CursorAnnotation))
            } else {
              tooltips
            }
          },
          provide = {
            showTooltip.computeN(arrayOf(it, RGAStateField)) { state -> state.toTooltips(it) }
          },
      ),
  )
}

private fun EventIdentifierArray.linearSearch(
    value: EventIdentifier,
): Int {
  var index = 0
  while (index < size) {
    if (get(index) == value) return index
    index++
  }
  return -1
}

private fun EditorState.toTooltips(f: StateField<CursorsState>): Array<Tooltip> {
  val rga = field(RGAStateField)
  val cursors = field(f)
  if (rga == undefined) return emptyArray()
  if (cursors == undefined) return emptyArray()

  // Compute the position of the cursors.
  val c = cursors.cursors.filter { it.actor != cursors.actor }
  return c
      .mapNotNull {
        val title = it.actor
        val pos = rga.identifiers.linearSearch(it.anchor).takeIf { i -> i >= 0 }
        pos?.let { p -> title to p }
      }
      .map { (title, pos) ->
        Tooltip(
            pos = pos + 1,
            create = {
              val dom = document.createElement("div") as HTMLElement
              dom.textContent = title.toString()
              TooltipView { this.dom = dom }
            }) {
          above = true
          strictSide = true
          asDynamic().`class` = "cm-cursor-tooltip"
        }
      }
      .toTypedArray()
}

val cursorTooltipBaseTheme =
    EditorView.baseTheme(
        js(
            """
    {
      '".cm-tooltip.cm-cursor-tooltip"': {
        backgroundColor: "#66b",
        color: "white",
        transform: "translate(-50%, -7px)",
        border: "none",
        padding: "2px 7px",
        borderRadius: "10px",
        '"&:before"': {
          position: "absolute",
          content: '""',
          left: "50%",
          marginLeft: "-5px",
          bottom: "-5px",
          borderLeft: "5px solid transparent",
          borderRight: "5px solid transparent",
          borderTop: "5px solid #66b"
        }
      }
    }
"""))
