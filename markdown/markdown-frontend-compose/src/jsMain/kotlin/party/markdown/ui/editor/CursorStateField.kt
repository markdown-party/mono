package party.markdown.ui.editor

import codemirror.state.Annotation
import codemirror.state.EditorState
import codemirror.state.StateField
import codemirror.state.StateFieldConfig
import codemirror.tooltip.Tooltip
import codemirror.tooltip.TooltipView
import codemirror.tooltip.showTooltip
import codemirror.view.EditorView
import io.github.alexandrepiveteau.echo.core.causality.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
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
            if (tr.annotation(CursorAnnotation) !== undefined) {
              tooltips.copy(cursors = tr.annotation(CursorAnnotation))
            } else {
              tooltips
            }
          },
          provide = {
            showTooltip.computeN(arrayOf(it, RGAStateField, NowStateField)) { state ->
              state.toTooltips(it)
            }
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
@OptIn(ExperimentalTime::class)
private fun EditorState.toTooltips(f: StateField<CursorsState>): Array<Tooltip> {
  val rga = field(RGAStateField)
  val cursors = field(f)
  if (rga == undefined) return emptyArray()
  if (cursors == undefined) return emptyArray()

  val now = field(NowStateField)

  // Compute the position of the cursors.
  val c = cursors.cursors.filter { it.actor != cursors.actor }
  return c
      .filter { it.timestamp + Delay >= now }
      .mapNotNull {
        val pos =
            rga.identifiers.indexOfCursor(it.anchor).takeIf { i -> i >= 0 }
                ?: return@mapNotNull null
        return@mapNotNull it.actor to pos
      }
      .map { (id, pos) ->
        val (icon, color) = id.toCursor()
        Tooltip(
            pos = pos,
            create = {
              val dom = kotlinx.browser.document.createElement("div") as HTMLElement
              dom.textContent = icon
              TooltipView { this.dom = dom }
            }) {
          above = true
          strictSide = true
          asDynamic().`class` = color
        }
      }
      .toTypedArray()
}

// THEMING AND STYLING OF THE CURSORS

// There are 9 possible icons and 8 colors. These numbers are relative primes, in order to maximize
// the number of possible combinations.

/** The delay before which the cursors get hidden. */
@ExperimentalTime private val Delay = Duration.seconds(5)

/** The tick-rate at which the [NowStateField] should be updated. */
@ExperimentalTime val DelayTick = Duration.seconds(1)

/**
 * Returns a [Pair] of of items from [CursorIcons] and [CursorColors] uniquely determined by this
 * [SiteIdentifier].
 */
fun SiteIdentifier.toCursor(): Pair<String, String> {
  val modIcon = toUInt().mod(CursorIcons.size.toUInt())
  val modColor = toUInt().mod(CursorColors.size.toUInt())
  val icon = CursorIcons[modIcon.toInt()]
  val color = CursorColors[modColor.toInt()]
  return icon to color
}

/** Some carefully selected icons. */
private val CursorIcons = listOf("🎃", "🚀", "🌈", "🎁", "🐳", "🐌", "🦄", "🍔", "👻")

/** Some carefully selected colors. Each color corresponds is in [cursorTooltipBaseTheme]. */
private val CursorColors =
    listOf(
        "cursor-red",
        "cursor-orange",
        "cursor-yellow",
        "cursor-green",
        "cursor-cyan",
        "cursor-blue",
        "cursor-purple",
        "cursor-brown",
    )

/** Maps the color from [CursorColors] to its TailwindCSS representation. */
fun colorToTailwind(color: String): String =
    when (color) {
      "cursor-red" -> "bg-red-500"
      "cursor-orange" -> "bg-yellow-700"
      "cursor-yellow" -> "bg-yellow-500" // Adjusted to fit the default TW palette.
      "cursor-green" -> "bg-green-500"
      "cursor-cyan" -> "bg-blue-400" // Adjusted to fit the default TW palette
      "cursor-blue" -> "bg-blue-500"
      "cursor-purple" -> "bg-purple-500"
      "cursor-brown" -> "bg-gray-500"
      else -> "#FFFFFF"
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
      '".cm-tooltip"': {
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
        }
      },
      '".cm-tooltip.cursor-red"': {
        backgroundColor: "#EC5F67",
        '"&:before"': {
          borderTop: "5px solid #EC5F67"
        }
      },
      '".cm-tooltip.cursor-orange"': {
        backgroundColor: "#F99157",
        '"&:before"': {
          borderTop: "5px solid #F99157"
        }
      },
      '".cm-tooltip.cursor-yellow"': {
        backgroundColor: "#FAC863",
        '"&:before"': {
          borderTop: "5px solid #FAC863"
        }
      },
      '".cm-tooltip.cursor-green"': {
        backgroundColor: "#99C794",
        '"&:before"': {
          borderTop: "5px solid #99C794"
        }
      },
      '".cm-tooltip.cursor-cyan"': {
        backgroundColor: "#62B3B2",
        '"&:before"': {
          borderTop: "5px solid #62B3B2"
        }
      },
      '".cm-tooltip.cursor-blue"': {
        backgroundColor: "#6699CC",
        '"&:before"': {
          borderTop: "5px solid #6699CC"
        }
      },
      '".cm-tooltip.cursor-purple"': {
        backgroundColor: "#C594C5",
        '"&:before"': {
          borderTop: "5px solid #C594C5"
        }
      },
      '".cm-tooltip.cursor-brown"': {
        backgroundColor: "#AB7967",
        '"&:before"': {
          borderTop: "5px solid #AB7967"
        }
      }
    }
"""))
