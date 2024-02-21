package party.markdown.tree

import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toSiteIdentifier
import io.github.alexandrepiveteau.echo.core.causality.toUInt
import io.github.alexandrepiveteau.echo.exchange
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.sync
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@State(Scope.Thread)
class MutableTreeBenchmark {

  @Param("2", "3", "4", "5", "6", "7", "8") var replicas = 0
  @Param("1", "2", "3", "5", "10", "100") var insertions = 0
  private val Files = 50

  private fun site(identifier: SiteIdentifier) =
      mutableSite(identifier, MutableTree(), TreeProjection)

  @Benchmark
  fun treeInsertions() = runBlocking {
    val primary = exchange()
    repeat(replicas) {
      val id = (SiteIdentifier.Min.toUInt() + it.toUInt()).toSiteIdentifier()
      val replica = site(id)
      val syncJob = launch { sync(replica, primary) }

      launch {
        repeat(insertions) {
          replica.event {
            val event = yield(TreeEvent.NewFile)
            yield(TreeEvent.Name(event, "file"))
          }
        }
        replica.value
            .map { mutable -> mutable.toTree() }
            .filterIsInstance<TreeNode.Folder>()
            .first { folder -> folder.children.size == replicas * insertions }
        syncJob.cancel()
      }
    }
  }

  @Benchmark
  fun treeMoves() = runBlocking {
    val primary = site(SiteIdentifier.Max)
    val folders = primary.event { List(Files) { yield(TreeEvent.NewFolder) } }

    repeat(replicas) {
      val id = (SiteIdentifier.Min.toUInt() + it.toUInt()).toSiteIdentifier()
      val replica = site(id)
      val syncJob = launch { sync(replica, primary) }

      launch {
        replica.event {
          repeat(insertions) {
            val from = folders.random()
            val into = folders.random()
            yield(TreeEvent.Move(from, into))
          }
        }
        replica.event { yield(TreeEvent.NewFile) }

        replica.value
            .map { mutable -> mutable.toTree() }
            .filterIsInstance<TreeNode.Folder>()
            .map { it.children.filterIsInstance<TreeNode.MarkdownFile>() }
            .first { it.size == replicas }
        syncJob.cancel()
      }
    }
  }
}
