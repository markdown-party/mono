package party.markdown

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.SyncStrategy
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.sync
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import party.markdown.cursors.CursorEvent
import party.markdown.rga.RGAEvent
import party.markdown.rga.RGANodeRoot
import party.markdown.tree.TreeEvent

class MarkdownPartyEventTest {

  @Test
  fun integration() = runTest {
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
    val a = alice.event { yield(MarkdownPartyEvent.RGA(file, RGAEvent.Insert(RGANodeRoot, 'a'))) }
    val b = alice.event { yield(MarkdownPartyEvent.RGA(file, RGAEvent.Insert(a, 'b'))) }
    alice.event {
      yield(MarkdownPartyEvent.RGA(file, RGAEvent.Insert(b, 'c')))
      yield(MarkdownPartyEvent.Cursor(CursorEvent.MoveAfter(file, b)))
      yield(MarkdownPartyEvent.Cursor(CursorEvent.MoveAfter(file, a)))
      yield(MarkdownPartyEvent.Cursor(CursorEvent.MoveAfter(file, b)))
      yield(MarkdownPartyEvent.Cursor(CursorEvent.MoveAfter(file, a)))
      yield(MarkdownPartyEvent.Cursor(CursorEvent.MoveAfter(file, b)))
    }
    sync(alice, bob)
    assertEquals(5, aliceHistory.size)
    assertContentEquals(charArrayOf('a', 'b', 'c'), bob.value.first().documents[file]?.first)
  }
}
