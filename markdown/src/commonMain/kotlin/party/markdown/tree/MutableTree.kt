package party.markdown.tree

import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.isSpecified
import io.github.alexandrepiveteau.echo.projections.ChangeScope

class MutableTree {

  private enum class NodeType {
    Folder,
    Text,
  }

  private val association = MutableEventIdentifierAssociationTable()
  private val graph = MutableIntGraph()
  private val names = mutableMapOf<EventIdentifier, String>()
  private val type = mutableMapOf<Int, NodeType>()

  private val root = graph.createVertex()

  private val folder = graph.createVertex()
  private val trash = graph.createVertex()

  init {
    // Keep a single tree, with two main sub-trees.
    graph.createEdge(root, folder)
    graph.createEdge(root, trash)

    type[root] = NodeType.Folder
    type[folder] = NodeType.Folder
    type[trash] = NodeType.Folder
  }

  /**
   * Checks some invariants on the current [MutableTree] structure, ensuring it is in a proper state
   * after applying the events.
   */
  private fun checkInvariants() {
    check(graph.isTree()) { "Tree invariant broken." }
    for (i in 0 until graph.size) {
      if (NodeType.Text == checkNotNull(type[i]) { "Missing type" }) {
        check(graph.neighboursSize(i) == 0) { "Non-folder node has some children." }
      }
    }
  }

  init {
    checkInvariants()
  }

  /**
   * Creates a new node with the given identifiers and type, and pushes the changes to the
   * [ChangeScope].
   *
   * @param id the identifier of the created node.
   * @param t the type of the created node.
   */
  private fun ChangeScope<TreeChange>.newNode(id: TreeNodeIdentifier, t: NodeType) {
    require(id.isSpecified) { "Can't insert the root folder." }
    require(!association.hasVertex(id)) { "Duplicate insertion for a single id." }

    val v = graph.createVertex()
    association.associate(v, id)
    graph.createEdge(root, v)
    type[v] = t

    push(TreeChange.RemoveVertex(v, id))
    push(TreeChange.RemoveEdge(root, v))
  }

  /** Creates a new file with the given [TreeNodeIdentifier] as identifier. */
  fun ChangeScope<TreeChange>.newFile(id: TreeNodeIdentifier) {
    newNode(id, NodeType.Text)
    checkInvariants()
  }

  /** Creates a new folder with the given [TreeNodeIdentifier] as identifier. */
  fun ChangeScope<TreeChange>.newFolder(id: TreeNodeIdentifier) {
    newNode(id, NodeType.Folder)
    checkInvariants()
  }

  /**
   * Moves the [TreeNode] with the given [TreeNodeIdentifier] to use the given [anchor] as its
   * parent.
   *
   * @param id the identifier of the moved node.
   * @param anchor the identifier of the anchor.
   */
  fun ChangeScope<TreeChange>.move(id: TreeNodeIdentifier, anchor: TreeNodeIdentifier) {
    // Only move if we have a vertex.
    if (!association.hasVertex(id)) return
    if (!association.hasVertex(anchor) && anchor != TreeNodeRoot) return
    val idVertex = association.vertex(id)
    val anchorVertex = if (anchor == TreeNodeRoot) folder else association.vertex(anchor)
    if (type[anchorVertex] != NodeType.Folder) return // you can't move files below folders.
    move(idVertex, anchorVertex)
  }

  private fun ChangeScope<TreeChange>.move(vertex: Int, anchor: Int) {
    if (!graph.moveWouldBreakTreeInvariant(vertex, anchor)) {
      val parent = graph.move(vertex, anchor)
      push(TreeChange.Move(vertex, parent))
    }
    checkInvariants()
  }

  /**
   * Changes the name of the file with the given [id].
   *
   * @param id the identifier of the renamed file.
   * @param name the newly set name.
   */
  fun ChangeScope<TreeChange>.name(id: TreeNodeIdentifier, name: String) {
    val previous = names[id]
    names[id] = name
    push(TreeChange.ChangeName(id, previous))
    checkInvariants()
  }

  /**
   * Removes the node with the given [TreeNodeIdentifier] from the hierarchy. Deleting a folder will
   * also result in the deletion of all its children, recursively.
   *
   * However, the children remain available and can be moved back to the graph as needed. This
   * allows for concurrent edits of the tree, where one site would move a child, and another site
   * would delete the folder.
   *
   * @param id the [TreeNodeIdentifier] to remove.
   */
  fun ChangeScope<TreeChange>.remove(id: TreeNodeIdentifier) {
    require(id.isSpecified) { "Can't delete the root folder." }

    // Conceptually, removing a node is like moving it to an unreachable root.
    if (association.hasVertex(id)) {
      val vertex = association.vertex(id)
      move(vertex, trash)
    }
    checkInvariants()
  }

  /** Applies the given [TreeChange], and reverses the state of the [MutableTree]. */
  fun backward(change: TreeChange) {
    when (change) {
      is TreeChange.CreateEdge -> {
        graph.createEdge(change.parent, change.child)
      }
      is TreeChange.ChangeName -> {
        if (change.name != null) names[change.identifier] = change.name
        else names.remove(change.identifier)
      }
      is TreeChange.Move -> {
        graph.move(change.vertex, change.anchor)
      }
      is TreeChange.RemoveEdge -> {
        graph.removeEdge(change.parent, change.child)
      }
      is TreeChange.RemoveVertex -> {
        type.remove(change.vertex)
        association.dissociate(change.vertex, change.identifier)
        graph.removeVertex(change.vertex)
      }
    }
    checkInvariants()
  }

  private fun toFolder(vertex: Int): TreeNode {
    val id = if (vertex == folder) TreeNodeRoot else association.identifier(vertex)
    return TreeNode.Folder(
        id = id,
        children = graph.neighbours(vertex).asSequence().map(this::toTree).toSet(),
        name = names[id],
    )
  }

  private fun toFile(vertex: Int): TreeNode {
    val id = association.identifier(vertex)
    return TreeNode.MarkdownFile(id, names[id])
  }

  /** Recursively aggregates the given [vertex] into a [TreeNode]. */
  private fun toTree(vertex: Int): TreeNode =
      when (type[vertex]) {
        NodeType.Folder -> toFolder(vertex)
        NodeType.Text -> toFile(vertex)
        null -> error("Unknown TreeNode type.")
      }

  /** Aggregates the tree of nodes into an immutable [TreeNode]. */
  fun toTree(): TreeNode {
    checkInvariants()
    return toTree(folder)
  }
}
