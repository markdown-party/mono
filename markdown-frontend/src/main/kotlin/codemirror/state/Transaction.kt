@file:JsNonModule
@file:JsModule("@codemirror/state")

package codemirror.state

import codemirror.text.Text

/**
 * Changes to the editor state are grouped into transactions. Typically, a user action creates a
 * single transaction, which may contain any number of document changes, may change the selection,
 * or have other effects. Create a transaction by calling [EditorState.update].
 *
 * This class provides some bindings to `https://codemirror.net/6/docs/ref/#state.Transaction`.
 */
external class Transaction {

  /** The state from which the transaction starts. */
  val startState: EditorState

  /** The document changes made by this transaction. */
  val changes: ChangeSet

  /**
   * The selection set by this transaction, or [undefined] if it doesn't explicitly set a selection.
   */
  val selection: EditorSelection

  /** The effects added to the transaction. */
  val effects: Array<StateEffect<Any>>

  /** Whether the selection should be scrolled into view after this transaction is dispatched. */
  val scrollIntoView: Boolean

  /**
   * The new document produced by the transaction. Contrary to .state.doc, accessing this won't
   * force the entire new state to be computed right away, so it is recommended that transaction
   * filters use this getter when they need to look at the new document.
   */
  val newDoc: Text

  /**
   * The new state created by the transaction. Computed on demand (but retained for subsequent
   * access), so it is recommended not to access it in transaction filters when possible.
   */
  val state: EditorState

  /** Get the value of the given annotation type, if any. */
  fun <T> annotation(type: AnnotationType<T>): T

  /** Indicates whether the transaction changed the document. */
  val docChanged: Boolean

  /**
   * Indicates whether this transaction reconfigures the state (through a configuration compartment
   * or with a top-level configuration effect.
   */
  val reconfigured: Boolean

  companion object {

    /**
     * Annotation used to associate a transaction with a user interface event. The view will set
     * this to :
     *
     * - "input" when the user types text
     * - "delete" when the user deletes the selection or text near the selection
     * - "keyboardselection" when moving the selection via the keyboard
     * - "pointerselection" when moving the selection through the pointing device
     * - "paste" when pasting content
     * - "cut" when cutting
     * - "drop" when content is inserted via drag-and-drop
     */
    val userEvent: AnnotationType<String>

    /** Annotation indicating whether a transaction should be added to the undo history or not. */
    val addToHistory: AnnotationType<Boolean>

    /**
     * Annotation indicating (when present and true) that a transaction represents a change made by
     * some other actor, not the user. This is used, for example, to tag other people's changes in
     * collaborative editing.
     */
    val remote: AnnotationType<Boolean>
  }
}
