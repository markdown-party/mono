package party.markdown.data.tree

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import party.markdown.tree.TreeNode
import party.markdown.tree.TreeNodeIdentifier

/**
 * An interface defining the API to interact with the tree of files, folders and documents that is
 * displayed to the user.
 */
interface TreeApi {

  /**
   * A [StateFlow] which gives access to the latest root [TreeNode], representing the collection of
   * documents and folders which the user may edit.
   */
  val current: Flow<TreeNode>

  /**
   * Creates a new file, with the provided [name] and the given [parent].
   *
   * If the [parent] isn't a folder or the move operation couldn't be performed, the file will be
   * added to the root folder.
   */
  suspend fun createFile(name: String, parent: TreeNode)

  /**
   * Creates a new folder, with the provided [name] and the given [parent].
   *
   * If the parent isn't a folder or the move operation can't be performed, the file will be added
   * to the root folder.
   */
  suspend fun createFolder(name: String, parent: TreeNode)

  /** Gives a certain name to the given node, provided its identifier. */
  suspend fun name(name: String, file: TreeNode)

  /**
   * Moves the file with the given [TreeNodeIdentifier] to be a child of the given [anchor]. If the
   * operation can't be performed, it will simply be skipped.
   */
  suspend fun move(node: TreeNodeIdentifier, anchor: TreeNode)

  /**
   * Deletes the given node, provided its identifier.
   *
   * If the node can't be deleted, the operation will be ignored.
   */
  suspend fun remove(file: TreeNode)
}
