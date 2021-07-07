package party.markdown.ui.navigator

import kotlinx.html.classes
import react.*
import react.dom.div

/**
 * Creates a new [ReactElement] that displays a file.
 *
 * @param block the configuration properties for that file.
 */
fun RBuilder.file(
    block: FileProps.() -> Unit,
): ReactElement = child(file) { attrs(block) }

/**
 * An interface defining the properties that can be used to display a file. Files may have a name,
 * and be part of a folder (or not).
 */
external interface FileProps : RProps {
  var name: String
  var selected: Boolean
  var isFolder: Boolean

  /** The indentation level by which this file should be displayed. */
  var indentLevel: Int
}

// COMPONENT

private val file =
    functionalComponent<FileProps> { props ->
      val text = if (props.isFolder) "Folder ${props.name}" else "File ${props.name}"

      // Display the indentation, then the text.
      div {
        attrs {
          classes = classes + setOf("border", "border-2", "border-black")
          classes = classes + setOf("flex", "flex-row", "justify-start")
          classes = classes + setOf("p-4")
          classes = classes + setOf("transition-all")
          classes = classes + setOf("cursor-pointer")
          if (props.selected) {
            classes = classes + setOf("text-blue-300")
            classes = classes + setOf("hover:text-blue-400")
          } else {
            classes = classes + setOf("hover:text-blue-200")
          }
        }
        for (i in 0 until props.indentLevel) {
          div { attrs { classes = classes + setOf("w-8") } }
        }
        +text
      }
    }
