package party.markdown.data.tree

import io.github.alexandrepiveteau.echo.MutableSite
import kotlinx.coroutines.flow.StateFlow
import party.markdown.tree.TreeEvent
import party.markdown.tree.TreeNode

/** An implementation of the [TreeApi] backed by a [MutableSite]. */
class MutableSiteTreeApi(
    private val site: MutableSite<TreeEvent, TreeNode>,
) : TreeApi {

  override val current: StateFlow<TreeNode> = site.value

  override suspend fun createFile(name: String, parent: TreeNode) =
      site.event {
        val id = yield(TreeEvent.NewFile)
        yield(TreeEvent.Name(id, name))
        yield(TreeEvent.Move(id, parent.id))
      }

  override suspend fun createFolder(name: String, parent: TreeNode) =
      site.event {
        val id = yield(TreeEvent.NewFolder)
        yield(TreeEvent.Name(id, name))
        yield(TreeEvent.Move(id, parent.id))
      }

  override suspend fun remove(file: TreeNode) = site.event { yield(TreeEvent.Remove(file.id)) }
}
