package party.markdown.ui.navigator

import kotlinx.html.classes
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.button
import react.dom.div
import react.dom.img
import react.dom.p

/**
 * Creates a new [ReactElement] that displays a file.
 *
 * @param block the configuration properties for that file.
 */
fun RBuilder.file(
    block: FileProps.() -> Unit,
): ReactElement = child(file) { attrs(block) }

enum class FileType {
  Markdown,
  FolderClosed,
  FolderOpen,
}

/**
 * An interface defining the properties that can be used to display a file. Files may have a name,
 * and be part of a folder (or not).
 */
external interface FileProps : RProps {
  var displayName: String
  var displaySelected: Boolean
  var displayFileType: FileType
  var displayIndentLevel: Int

  var menuOpen: Boolean

  var onClick: () -> Unit
  var onMenuClick: () -> Unit
  var onMenuRenameClick: () -> Unit
  var onMenuDeleteClick: () -> Unit
}

// COMPONENT

private fun FileProps.iconUrl(): String =
    when (displayFileType) {
      FileType.Markdown -> "/icons/navigator-file-markdown.svg"
      FileType.FolderClosed -> "/icons/navigator-folder-closed.svg"
      FileType.FolderOpen -> "/icons/navigator-folder-open.svg"
    }

private val file =
    functionalComponent<FileProps> { props ->
      div(
          """
          flex flex-row justify-start items-center space-x-2
          px-2
          transition-all
          cursor-pointer
          ${if (props.displaySelected) "text-blue-300 hover:text-blue-400" else "hover:text-blue-200"}
          group
          """,
      ) {
        attrs { onClickFunction = { props.onClick() } }
        for (i in 0 until props.displayIndentLevel) {
          div(classes = "w-4") {}
        }
        img(src = props.iconUrl()) {}
        p(classes = "flex-grow") { +props.displayName }
        div("""
            inline-block
            relative
            """) {
          img(
              classes =
                  """
                transition-all
                opacity-0 group-hover:opacity-50
                hover:bg-gray-800
                rounded p-2
                """,
              src = "/icons/navigator-more.svg",
          ) { attrs { onClickFunction = { props.onMenuClick() } } }
          if (props.menuOpen) {
            div(
                """
                origin-top-right absolute right-0 mt-2 w-56 rounded-md shadow-lg bg-white
                ring-1 ring-black ring-opacity-5 focus:outline-none
                z-10 text-black text-semibold
                flex flex-col items-stretch text-lg p-2 space-y-1 text-left
                """,
            ) {
              button {
                attrs {
                  classes = setOf("transition-all hover:bg-gray-200 rounded p-2")
                  onClickFunction = { props.onMenuRenameClick() }
                }
                +"Rename"
              }
              button {
                attrs {
                  classes = setOf("transition-all hover:bg-red-200 rounded text-red-600 p-2")
                  onClickFunction = { props.onMenuDeleteClick() }
                }
                +"Delete"
              }
            }
          }
        }
      }
    }
