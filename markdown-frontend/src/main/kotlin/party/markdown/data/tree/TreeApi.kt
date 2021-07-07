package party.markdown.data.tree

import kotlinx.coroutines.flow.StateFlow
import party.markdown.tree.TreeNode

/**
 * An interface defining the API to interact with the tree of files, folders and documents that is
 * displayed to the user.
 */
interface TreeApi {

  /**
   * A [StateFlow] which gives access to the latest root [TreeNode], representing the collection of
   * documents and folders which the user may edit.
   */
  val current: StateFlow<TreeNode>

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

  /**
   * Deletes the given node, provided its identifier.
   *
   * If the node can't be deleted, the operation will be ignored.
   */
  suspend fun remove(file: TreeNode)
}
