package party.markdown

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.sync
import io.github.alexandrepiveteau.echo.sync.SyncStrategy
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import party.markdown.MarkdownPartyEvent.Cursor
import party.markdown.MarkdownPartyEvent.RGA
import party.markdown.cursors.CursorEvent.MoveAfter
import party.markdown.rga.RGAEvent.Insert
import party.markdown.rga.RGANodeRoot
import party.markdown.tree.TreeEvent

/** All the test cases that can not run on Kotlin/JS and only target Kotlin/JVM for now. */
class Bugs {

  @Test
  fun simple() = runTest {
    val aliceHistory = MarkdownPartyHistory()
    val alice: MutableSite<MarkdownPartyEvent, MarkdownParty> =
        mutableSite(
            identifier = SiteIdentifier.Min,
            history = aliceHistory,
            transform = MutableMarkdownParty::toMarkdownParty,
        )
    val bob: MutableSite<MarkdownPartyEvent, MarkdownParty> =
        mutableSite(
            identifier = SiteIdentifier.Max,
            history = MarkdownPartyHistory(),
            transform = MutableMarkdownParty::toMarkdownParty,
            strategy = SyncStrategy.Once,
        )

    val file = alice.event { yield(MarkdownPartyEvent.Tree(TreeEvent.NewFile)) }
    val a = alice.event { yield(RGA(file, Insert(RGANodeRoot, 'a'))) }
    val b = alice.event { yield(RGA(file, Insert(a, 'b'))) }
    alice.event {
      yield(RGA(file, Insert(b, 'c')))
      yield(Cursor(MoveAfter(file, b)))
      yield(Cursor(MoveAfter(file, a)))
      yield(Cursor(MoveAfter(file, b)))
      yield(Cursor(MoveAfter(file, a)))
      yield(Cursor(MoveAfter(file, b)))
    }
    sync(alice, bob)
    assertEquals(5, aliceHistory.size)
    assertContentEquals(charArrayOf('a', 'b', 'c'), bob.value.documents[file]?.first)
  }
}
