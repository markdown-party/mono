package party.markdown.tree

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("c")
@Serializable
sealed class TreeChange {

  @SerialName("v")
  @Serializable
  data class RemoveVertex(
      val vertex: Int,
      val identifier: TreeNodeIdentifier,
  ) : TreeChange()

  @SerialName("e")
  @Serializable
  data class CreateEdge(
      val parent: Int,
      val child: Int,
  ) : TreeChange()

  @SerialName("m")
  @Serializable
  data class Move(
      val vertex: Int,
      val anchor: Int?,
  ) : TreeChange()

  @SerialName("n")
  @Serializable
  data class ChangeName(
      val identifier: TreeNodeIdentifier,
      val name: String?,
  ) : TreeChange()
}
