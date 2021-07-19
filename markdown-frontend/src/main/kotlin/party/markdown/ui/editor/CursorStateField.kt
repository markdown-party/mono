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
import io.github.alexandrepiveteau.echo.core.causality.isUnspecified
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import party.markdown.cursors.Cursors

/** An [Annotation] that lets transactions overwrite the available cursors. */
val CursorAnnotation = Annotation.define<Set<Cursors.Cursor>>()

/**
 * The state of the [cursorsStateField]. It contains the current [actor], which will be masked from
 * the list of cursors, and the [Set] of [Cursors.Cursor] from all the other sites, which will be
 * displayed if they are at a known position.
 */
data class CursorsState(
    val actor: SiteIdentifier,
    val cursors: Set<Cursors.Cursor>,
)

/**
 * Creates a new [StateField] which will provide access to a [CursorsState]. The [StateField]
 * accepts transactions with the [CursorAnnotation], and will update its state depending on the
 * cursors provided in this state.
 *
 * Additionally, the current [actor] will be hidden from the displayed cursors.
 */
fun cursorsStateField(
    actor: SiteIdentifier,
): StateField<CursorsState> {
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

/**
 * Finds the index of the given [EventIdentifier] in this [EventIdentifierArray]. The
 * [EventIdentifierArray] might be unsorted.
 *
 * @param value the [EventIdentifier] that we're searching.
 */
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

/**
 * Finds the index of the anchor in the provided [EventIdentifierArray]. If the index is negative, a
 * `null` value will be returned.
 *
 * Additionally, if the [identifier] is [EventIdentifier.Unspecified], the index will be set to `0`,
 * since it means that the cursor is at the beginning of the text.
 */
fun EventIdentifierArray.indexOfCursor(
    identifier: EventIdentifier,
): Int {
  if (identifier.isUnspecified) return 0
  return linearSearch(identifier).takeIf { it >= 0 }?.plus(1) ?: -1
}

/**
 * Maps the current [EditorState] to the [Array] of [Tooltip] that should be displayed by the text
 * editor.
 *
 * @param f the [StateField] instance on which we're acting.
 */
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
        if (it.anchor.isUnspecified) return@mapNotNull title to 0
        val pos =
            rga.identifiers.indexOfCursor(it.anchor).takeIf { i -> i >= 0 }
                ?: return@mapNotNull null
        return@mapNotNull title to pos
      }
      .map { (title, pos) ->
        Tooltip(
            pos = pos,
            create = {
              val dom = document.createElement("div") as HTMLElement
              dom.textContent = "🎉"//title.toString()
              TooltipView { this.dom = dom }
            }) {
          above = true
          strictSide = true
          asDynamic().`class` = "cm-cursor-tooltip"
        }
      }
      .toTypedArray()
}

/**
 * The base theme of the cursors, that will be applied to all the cursors created with the
 * `cm-cursor-tooltip` class.
 */
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
