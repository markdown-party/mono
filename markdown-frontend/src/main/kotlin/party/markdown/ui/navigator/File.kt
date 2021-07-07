package party.markdown.ui.navigator

import react.*
import react.dom.br

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
  var isFolder: Boolean

  /** The indentation level by which this file should be displayed. */
  var indentLevel: Int
}

// COMPONENT

private val file =
    functionalComponent<FileProps> { props ->
      val indent = if (props.indentLevel > 0) "+" + "-".repeat(props.indentLevel - 1) else ""
      val text = if (props.isFolder) "Folder ${props.name}" else "File ${props.name}"

      // Display the indentation, then the text.
      +indent
      +text
      br {}
    }
