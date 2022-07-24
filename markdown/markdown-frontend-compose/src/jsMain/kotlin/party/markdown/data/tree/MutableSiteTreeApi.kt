package party.markdown.data.tree

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import party.markdown.MarkdownParty
import party.markdown.MarkdownPartyEvent
import party.markdown.tree.TreeEvent
import party.markdown.tree.TreeNode
import party.markdown.tree.TreeNodeIdentifier

/** An implementation of the [TreeApi] backed by a [MutableSite]. */
class MutableSiteTreeApi(
    private val site: MutableSite<MarkdownPartyEvent, MarkdownParty>,
) : TreeApi {

  override val current: Flow<TreeNode> = site.value.map { it.tree }

  override suspend fun createFile(name: String, parent: TreeNode): Unit =
      site.event {
        val id = yield(MarkdownPartyEvent.Tree(TreeEvent.NewFile))
        yield(MarkdownPartyEvent.Tree(TreeEvent.Name(id, name)))
        yield(MarkdownPartyEvent.Tree(TreeEvent.Move(id, parent.id)))
      }

  override suspend fun createFolder(name: String, parent: TreeNode): Unit =
      site.event {
        val id = yield(MarkdownPartyEvent.Tree(TreeEvent.NewFolder))
        yield(MarkdownPartyEvent.Tree(TreeEvent.Name(id, name)))
        yield(MarkdownPartyEvent.Tree(TreeEvent.Move(id, parent.id)))
      }

  override suspend fun name(name: String, file: TreeNode): Unit =
      site.event { yield(MarkdownPartyEvent.Tree(TreeEvent.Name(file.id, name))) }

  override suspend fun move(node: TreeNodeIdentifier, anchor: TreeNode): Unit =
      site.event {
        yield(MarkdownPartyEvent.Tree(TreeEvent.Move(element = node, anchor = anchor.id)))
      }

  override suspend fun remove(file: TreeNode): Unit =
      site.event { yield(MarkdownPartyEvent.Tree(TreeEvent.Remove(file.id))) }
}
