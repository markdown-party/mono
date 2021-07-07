package party.markdown.tree

import kotlinx.serialization.Serializable

@Serializable
sealed class TreeChange {

  @Serializable
  data class RemoveVertex(
      val vertex: Int,
      val identifier: TreeNodeIdentifier,
  ) : TreeChange()

  @Serializable
  data class CreateEdge(
      val parent: Int,
      val child: Int,
  ) : TreeChange()

  @Serializable
  data class Move(
      val vertex: Int,
      val anchor: Int?,
  ) : TreeChange()


  @Serializable
  data class ChangeName(
      val identifier: TreeNodeIdentifier,
      val name: String?,
  ) : TreeChange()
}
