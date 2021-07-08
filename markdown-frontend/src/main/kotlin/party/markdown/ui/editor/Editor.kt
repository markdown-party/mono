package party.markdown.ui.editor

import react.*
import react.dom.div

fun RBuilder.editor(
    block: EditorProps.() -> Unit,
): ReactElement = child(editor) { attrs(block) }

external interface EditorProps : RProps

private val editor = functionalComponent<EditorProps> { div(classes = "bg-green-500 flex-grow") {} }
