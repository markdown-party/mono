package party.markdown.cursors

import io.github.alexandrepiveteau.echo.MutableSite
import io.github.alexandrepiveteau.echo.core.causality.SiteIdentifier
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.sync
import io.github.alexandrepiveteau.echo.sync.SyncStrategy
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import party.markdown.*
import party.markdown.rga.RGAEvent
import party.markdown.rga.RGANodeRoot
import party.markdown.tree.TreeEvent

class CompactionTest {

  @Test
  fun simple() = suspendTest {
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
    alice.event {
      val a =
          yield(
              MarkdownPartyEvent.RGA(
                  document = file,
                  event = RGAEvent.Insert(offset = RGANodeRoot, atom = 'a'),
              ),
          )
      val b =
          yield(
              MarkdownPartyEvent.RGA(
                  document = file,
                  event = RGAEvent.Insert(offset = a, atom = 'b'),
              ),
          )
      yield(
          MarkdownPartyEvent.RGA(
              document = file,
              event = RGAEvent.Insert(offset = b, atom = 'c'),
          ),
      )
      yield(MarkdownPartyEvent.Cursor(CursorEvent.MoveAfter(file, b)))
      yield(MarkdownPartyEvent.Cursor(CursorEvent.MoveAfter(file, a)))
      yield(MarkdownPartyEvent.Cursor(CursorEvent.MoveAfter(file, b)))
      yield(MarkdownPartyEvent.Cursor(CursorEvent.MoveAfter(file, a)))
      yield(MarkdownPartyEvent.Cursor(CursorEvent.MoveAfter(file, b)))
    }
    sync(alice, bob)
    assertEquals(5, aliceHistory.size)
    assertContentEquals(charArrayOf('a', 'b', 'c'), bob.value.value.documents[file]?.first)
  }
}
