package party.markdown.tree

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A [TreeNodeIdentifier] identifiers a node in a tree replicated data type. All nodes have a
 * distinct [TreeNodeIdentifier].
 */
typealias TreeNodeIdentifier = EventIdentifier

/**
 * The root [TreeNodeIdentifier], which indicates that the reference is the top-level folder (in
 * which all files and folders are created by default).
 */
val TreeNodeRoot = TreeNodeIdentifier.Unspecified

/**
 * A [TreeEvent] defines the operations which are available on a replicated tree : creating files,
 * creating folders, moving them around, renaming them, and deleting them.
 *
 * These operations operate on some `element`, and might reference an `anchor`. The `element` will
 * be the identifier of the node that is affected, and the `anchor` may indicate a destination.
 *
 * Operations which are not compatible with the current tree structure will simply be skipped.
 */
@Serializable
sealed class TreeEvent {

  /**
   * Creates a new file at the root of the tree. This file will not have any parent, and will be
   * populated with a default name.
   */
  @Serializable @SerialName("f:fi") object NewFile : TreeEvent()

  /**
   * Creates a new folder at the root of the tree. This folder will not have any parent, and will be
   * populated with a default name.
   */
  @Serializable @SerialName("f:fo") object NewFolder : TreeEvent()

  // TODO : Upload image operation

  /**
   * Moves the file or folder reference to be a child of the given [anchor]. If the file was to be
   * moved at the top-level, the [TreeNodeRoot] value should be specified as the [anchor].
   *
   * If the [Move] operation was to result in a cycle, it should be skipped and not affect the data
   * structure. Moreover, if the [anchor] does not reference a valid folder or [TreeNodeRoot], the
   * operation should be skipped as well.
   *
   * Because tree events are applied with a two-way projection, an valid operation may become
   * invalid later, or vice-versa. Therefore, it's not necessary to "force" convergence and one may
   * simply skip operations.
   *
   * @param element the reference to the moved element.
   * @param anchor the reference to the folder to which the file is moved.
   */
  @Serializable
  @SerialName("f:m")
  data class Move(
      val element: TreeNodeIdentifier,
      val anchor: TreeNodeIdentifier,
  ): TreeEvent()

  /**
   * Names the given file or folder. If multiple files or folders have the same name, an artificial
   * suffix will be given to them.
   *
   * @param element the reference to the named element.
   * @param name the new name.
   */
  @Serializable
  @SerialName("f:n")
  data class Name(
      val element: TreeNodeIdentifier,
      val name: String,
  ): TreeEvent()

  /**
   * Deletes the given file or folder. If a folder is deleted, its children will also be deleted
   * recursively. Concurrent moves of the children should not result in the children being deleted;
   * rather, a [Remove] operation could be seen as moving the file to a separate root of deleted and
   * unavailable files.
   *
   * @param element the root of the folder to remove, or the reference to the file to remove.
   */
  @Serializable
  @SerialName("f:r")
  data class Remove(
      val element: TreeNodeIdentifier,
  ): TreeEvent()
}
