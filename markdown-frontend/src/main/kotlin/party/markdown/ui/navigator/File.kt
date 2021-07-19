package party.markdown.ui.navigator

import kotlinx.html.Draggable
import kotlinx.html.draggable
import kotlinx.html.js.*
import react.*
import react.dom.*

/**
 * Creates a new [ReactElement] that displays a file.
 *
 * @param block the configuration properties for that file.
 */
fun RBuilder.file(
    block: FileProps.() -> Unit,
): ReactElement = child(file) { attrs(block) }

enum class FileType(val isFolder: Boolean) {
  Markdown(false),
  FolderClosed(true),
  FolderOpen(true),
}

/**
 * An interface defining the properties that can be used to display a file. Files may have a name,
 * and be part of a folder (or not).
 */
external interface FileProps : RProps {
  var displayId: Long
  var displayName: String
  var displaySelected: Boolean
  var displayFileType: FileType
  var displayIndentLevel: Int

  var menuOpen: Boolean

  var onDropFile: (Long) -> Unit

  var onFileClick: () -> Unit
  var onMenuClick: () -> Unit
  var onMenuMoveToParent: () -> Unit
  var onRenamed: (String) -> Unit
  var onMenuDeleteClick: () -> Unit

  // Only displayed when the displayFileType is FolderClosed or FolderOpen
  var onMenuCreateMarkdownClick: () -> Unit
  var onMenuCreateFolderClick: () -> Unit
}

// COMPONENT

private fun FileProps.iconUrl(): String =
    when (displayFileType) {
      FileType.Markdown -> "/icons/navigator-file-markdown.svg"
      FileType.FolderClosed -> "/icons/navigator-folder-closed.svg"
      FileType.FolderOpen -> "/icons/navigator-folder-open.svg"
    }

private fun RBuilder.dropdownButton(
    text: String,
    icon: String,
    onClick: () -> Unit,
    classes: String = "",
): ReactElement =
    button(
        classes = "transition-all rounded p-2 $classes flex flex-row items-center",
    ) {
      attrs {
        onClickFunction =
            {
              onClick()
              it.stopPropagation()
            }
      }
      img(src = icon, alt = text, classes = "pr-4") {}
      +text
    }

private val file =
    functionalComponent<FileProps> { props ->
      val (editedName, setEditedName) = useState<String?>(null)
      div(
          """
          flex flex-row justify-start items-center space-x-2
          px-4 py-1
          transition-all
          cursor-pointer
          ${if (props.displaySelected) "text-blue-300 hover:text-blue-400" else "hover:text-blue-200"}
          group
          """,
      ) {
        attrs {
          onClickFunction = { props.onFileClick() }
          draggable = Draggable.htmlTrue
          onDragStartFunction =
              { event ->
                event.asDynamic().dataTransfer.setData("text", props.displayId.toString())
                Unit
              }
          onDragOverFunction =
              { event ->
                if (props.displayFileType.isFolder) event.preventDefault()
              }
          onDropFunction =
              { event ->
                event.preventDefault()
                val id = (event.asDynamic().dataTransfer.getData("text") as? String)?.toLongOrNull()
                if (id != null) props.onDropFile(id)
              }
        }
        for (i in 0 until props.displayIndentLevel) {
          div(classes = "w-4") {}
        }
        img(src = props.iconUrl()) {}
        if (editedName != null) {
          input(
              classes =
                  """transition-all
                 bg-gray-700 border-gray-600 hover:bg-gray-600 text-white
                 px-2 py-1 rounded
                 text-lg font-medium
                 appearance-none focus:outline-none focus:border-gray-400 border-2
                 """,
          ) {
            attrs {
              this.value = editedName
              this.onChangeFunction = { setEditedName(it.target.asDynamic().value as? String) }
            }
          }
          button {
            img(
                src = "/icons/project-name-save.svg",
                classes = "rounded-lg border-2 border-gray-600 hover:bg-gray-600 p-1") {
              attrs {
                onClickFunction =
                    {
                      props.onRenamed(editedName)
                      setEditedName(null)
                    }
              }
            }
          }
        } else {
          p(classes = "flex-grow select-none") { +props.displayName }

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
            ) {
              attrs {
                onClickFunction =
                    {
                      props.onMenuClick()
                      it.stopPropagation()
                    }
              }
            }
            if (props.menuOpen) {
              div(
                  """
                origin-top-right absolute right-0 mt-2 w-64 rounded-md shadow-lg bg-white
                ring-1 ring-black ring-opacity-5 focus:outline-none
                z-10 text-black text-semibold
                flex flex-col items-stretch text-lg p-2 space-y-1 text-left
                """,
              ) {
                if (props.displayFileType.isFolder) {
                  dropdownButton(
                      text = "New Markdown file",
                      icon = "/icons/navigator-dropdown-new-text.svg",
                      onClick = props.onMenuCreateMarkdownClick,
                      classes = "hover:bg-gray-200",
                  )
                  dropdownButton(
                      text = "New Folder",
                      icon = "/icons/navigator-dropdown-new-folder.svg",
                      onClick = props.onMenuCreateFolderClick,
                      classes = "hover:bg-gray-200",
                  )
                }
                dropdownButton(
                    text = "Rename",
                    icon = "/icons/navigator-dropdown-rename.svg",
                    onClick = {
                      props.onMenuClick()
                      setEditedName(props.displayName)
                    },
                    classes = "hover:bg-gray-200",
                )
                if (props.displayIndentLevel > 0) {
                  dropdownButton(
                      text = "Move to parent",
                      icon = "/icons/navigator-dropdown-move-to-parent.svg",
                      onClick = props.onMenuMoveToParent,
                      classes = "hover:bg-gray-200",
                  )
                }
                dropdownButton(
                    text = "Delete",
                    icon = "/icons/navigator-dropdown-delete.svg",
                    onClick = props.onMenuDeleteClick,
                    classes = "hover:bg-red-200 text-red-600",
                )
              }
            }
          }
        }
      }
    }
