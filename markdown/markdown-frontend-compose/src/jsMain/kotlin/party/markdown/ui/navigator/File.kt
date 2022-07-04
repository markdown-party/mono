package party.markdown.ui.navigator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.attributes.Draggable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLButtonElement

enum class FileType(val isFolder: Boolean) {
  Markdown(false),
  FolderClosed(true),
  FolderOpen(true),
}

private fun FileType.iconUrl(): String =
    when (this) {
      FileType.Markdown -> "/icons/navigator-file-markdown.svg"
      FileType.FolderClosed -> "/icons/navigator-folder-closed.svg"
      FileType.FolderOpen -> "/icons/navigator-folder-open.svg"
    }

@Composable
fun DropdownButton(
    text: String,
    icon: String,
    onClick: () -> Unit,
    attrs: AttrBuilderContext<HTMLButtonElement>? = null,
) {
  Button(
      attrs = {
        classes("transition-all", "rounded", "p-2")
        if (attrs != null) attrs()
        classes("flex", "flex-row", "items-center")
        onClick {
          onClick()
          it.stopPropagation()
        }
      },
  ) {
    Img(src = icon, alt = text, attrs = { classes("pr-4") })
    Text(text)
  }
}

@Composable
fun File(
    name: String,
    displayId: Long,
    displayName: String,
    displaySelected: Boolean,
    displayFileType: FileType,
    displayIndentLevel: Int,
    menuOpen: Boolean,
    onDropFile: (Long) -> Unit,
    onFileClick: () -> Unit,
    onMenuClick: () -> Unit,
    onMenuMoveToParent: () -> Unit,
    onRenamed: (String) -> Unit,
    onMenuDeleteClick: () -> Unit,
    onMenuCreateMarkdownClick: () -> Unit,
    onMenuCreateFolderClick: () -> Unit,
) {
  val (editedName, setEditedName) = remember { mutableStateOf<String?>(null) }
  Div(
      attrs = {
        classes("flex", "flex-row", "justify-start", "items-center", "space-x-2")
        classes("px-4", "py-1")
        classes("transition-all")
        classes("cursor-pointer")
        if (displaySelected) {
          classes("text-blue-300", "hover:text-blue-400")
        } else {
          classes("hover:text-blue-200")
        }
        classes("group")
        onClick { onFileClick() }
        draggable(Draggable.True)
        onDragStart { event -> event.dataTransfer?.setData("text", displayId.toString()) }
        onDragOver { if (displayFileType.isFolder) it.preventDefault() }
        onDrop { event ->
          event.preventDefault()
          val id = event.dataTransfer?.getData("text")?.toLongOrNull()
          if (id != null) onDropFile(id)
        }
      },
  ) {
    for (i in 0 until displayIndentLevel) {
      Div(attrs = { classes("w-4") })
    }
    Img(src = displayFileType.iconUrl())
    if (editedName != null) {
      Input(
          type = InputType.Text,
          attrs = {
            classes("transition-all")
            classes("bg-gray-700", "border-gray-600", "hover:bg-gray-600", "text-white")
            classes("px-2", "py-1", "rounded")
            classes("text-lg", "font-medium")
            classes("appearance-none", "focus:outline-none", "focus:border-gray-400", "border-2")
            value(editedName)
            onInput { setEditedName(it.value) }
          },
      )
      Button {
        Img(
            src = "/icons/project-name-save.svg",
            attrs = {
              classes("rounded-lg", "border-2", "border-gray-600", "hover:bg-gray-600", "p-1")
              onClick {
                onRenamed(editedName)
                setEditedName(null)
              }
            },
        )
      }
    } else {
      P(attrs = { classes("flex-grow", "select-none") }) { Text(displayName) }
      Div(attrs = { classes("inline-block", "relative") }) {
        Img(
            src = "/icons/navigator-more.svg",
            attrs = {
              classes("transition-all")
              classes("opacity-0", "group-hover:opacity-50")
              classes("hover:bg-gray-800")
              classes("rounded", "p-2")
              onClick {
                onMenuClick()
                it.stopPropagation()
              }
            },
        )
        if (menuOpen) {
          Div(
              attrs = {
                classes(
                    "origin-top-right",
                    "absolute",
                    "right-0",
                    "mt-2",
                    "w-64",
                    "rounded-md",
                    "shadow-lg",
                    "bg-white",
                )
                classes("ring-1", "ring-black", "ring-opacity-5", "focus:outline-none")
                classes("z-10", "text-black", "text-semibold")
                classes(
                    "flex", "flex-col", "items-stretch", "text-lg", "p-2", "space-y-1", "text-left")
              },
          ) {
            if (displayFileType.isFolder) {
              DropdownButton(
                  text = "New Markdown file",
                  icon = "/icons/navigator-dropdown-new-text.svg",
                  onClick = onMenuCreateMarkdownClick,
                  attrs = { classes("hover:bg-gray-200") },
              )
              DropdownButton(
                  text = "New Folder",
                  icon = "/icons/navigator-dropdown-new-folder.svg",
                  onClick = onMenuCreateFolderClick,
                  attrs = { classes("hover:bg-gray-200") },
              )
            }
            DropdownButton(
                text = "Rename",
                icon = "/icons/navigator-dropdown-rename.svg",
                onClick = {
                  onMenuClick()
                  setEditedName(name)
                },
                attrs = { classes("hover:bg-gray-200") },
            )
            if (displayIndentLevel > 0) {
              DropdownButton(
                  text = "Move to parent",
                  icon = "/icons/navigator-dropdown-move-to-parent.svg",
                  onClick = onMenuMoveToParent,
                  attrs = { classes("hover:bg-gray-200") },
              )
            }
            DropdownButton(
                text = "Delete",
                icon = "/icons/navigator-dropdown-delete.svg",
                onClick = onMenuDeleteClick,
                attrs = { classes("hover:bg-red-200", "text-red-600") },
            )
          }
        }
      }
    }
  }
}
