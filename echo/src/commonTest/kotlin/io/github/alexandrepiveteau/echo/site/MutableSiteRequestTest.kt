package io.github.alexandrepiveteau.echo.site

import app.cash.turbine.test
import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber.Companion.Zero
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier.Companion.random
import io.github.alexandrepiveteau.echo.link
import io.github.alexandrepiveteau.echo.logs.persistentEventLogOf
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import io.github.alexandrepiveteau.echo.site
import io.github.alexandrepiveteau.echo.suspendTest
import io.github.alexandrepiveteau.echo.sync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MutableSiteRequestTest {

  @Test
  fun noRequest_issuesNoEvents() = suspendTest {
    val id = EventIdentifier(Zero, random())
    val event = id to 1
    val site = site(persistentEventLogOf(event))
    val link =
        link<Inc<Int>, Out<Int>> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id.site), expectItem())
            assertEquals(Inc.Ready, expectItem())
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun onlyAck_issuesNoEvents() = suspendTest {
    val id = EventIdentifier(Zero, random())
    val event = id to 1
    val site = site(persistentEventLogOf(event))
    val link =
        link<Inc<Int>, Out<Int>> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id.site), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Acknowledge(id.site, Zero))
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun onlyRequest_issuesNoEvents() = suspendTest {
    val id = EventIdentifier(Zero, random())
    val event = id to 1
    val site = site(persistentEventLogOf(event))
    val link =
        link<Inc<Int>, Out<Int>> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id.site), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Request(id.site, 10U))
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun ackThenRequest_zero_issuesNoEvent() = suspendTest {
    val id = EventIdentifier(Zero, random())
    val event = id to 1
    val site = site(persistentEventLogOf(event))
    val link =
        link<Inc<Int>, Out<Int>> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id.site), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Acknowledge(id.site, Zero))
            emit(Out.Request(id.site, 0U))
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun ackThenRequest_issuesEvent() = suspendTest {
    val id = EventIdentifier(Zero, random())
    val event = id to 1
    val site = site(persistentEventLogOf(event))
    val link =
        link<Inc<Int>, Out<Int>> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id.site), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Acknowledge(id.site, Zero))
            emit(Out.Request(id.site, 1U))
            assertEquals(Inc.Event(id.seqno, id.site, 1), expectItem())
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun requestThenAck_issuesNoEvent() = suspendTest {
    val id = EventIdentifier(Zero, random())
    val event = id to 1
    val site = site(persistentEventLogOf(event))
    val link =
        link<Inc<Int>, Out<Int>> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id.site), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Request(id.site, 1U))
            emit(Out.Acknowledge(id.site, Zero))
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun multipleRequest_sum() = suspendTest {
    val id = random()
    val id1 = EventIdentifier(Zero, id)
    val id2 = EventIdentifier(Zero.inc(), id)
    val event1 = id1 to 1
    val event2 = id2 to 2
    val site = site(persistentEventLogOf(event1, event2))
    val link =
        link<Inc<Int>, Out<Int>> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Acknowledge(id, Zero))
            // Sum two requests before expecting items.
            emit(Out.Request(id, 1U))
            emit(Out.Request(id, 1U))
            assertEquals(Inc.Event(id1.seqno, id1.site, 1), expectItem())
            assertEquals(Inc.Event(id2.seqno, id2.site, 2), expectItem())
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun multipleRequest_interleaved_sum() = suspendTest {
    val id = random()
    val id1 = EventIdentifier(Zero, id)
    val id2 = EventIdentifier(Zero.inc(), id)
    val event1 = id1 to 1
    val event2 = id2 to 2
    val site = site(persistentEventLogOf(event1, event2))
    val link =
        link<Inc<Int>, Out<Int>> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Acknowledge(id, Zero))
            // Interleave item reception and requests.
            emit(Out.Request(id, 1U))
            assertEquals(Inc.Event(id1.seqno, id1.site, 1), expectItem())
            emit(Out.Request(id, 1U))
            assertEquals(Inc.Event(id2.seqno, id2.site, 2), expectItem())
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun ackThenRequest() = suspendTest {
    val id = EventIdentifier(Zero, random())
    val site = site<Int>()

    val link =
        link<Out<Int>, Inc<Int>> { inc ->
          inc.test {
            emit(Inc.Advertisement(id.site))
            emit(Inc.Ready)
            assertEquals(Out.Acknowledge(id.site, Zero), expectItem())
            val request = expectItem() as Out.Request
            assertTrue(request.site == id.site && request.count >= 1U)
            expectNoEvents()
          }
        }
    sync(site.outgoing(), link)
  }

  @Test
  fun requestOverflow_isHandled() = suspendTest {
    val id = random()
    val id1 = EventIdentifier(Zero, id)
    val id2 = EventIdentifier(Zero.inc(), id)
    val event1 = id1 to 1
    val event2 = id2 to 2
    val site = site(persistentEventLogOf(event1, event2))
    val link =
      link<Inc<Int>, Out<Int>> { inc ->
        inc.test {
          assertEquals(Inc.Advertisement(id), expectItem())
          assertEquals(Inc.Ready, expectItem())
          emit(Out.Acknowledge(id, Zero))
          // Sum two requests such that their overflowing total is 0U before expecting items.
          emit(Out.Request(id, UInt.MAX_VALUE))
          emit(Out.Request(id, 1U))
          assertEquals(Inc.Event(id1.seqno, id1.site, 1), expectItem())
          assertEquals(Inc.Event(id2.seqno, id2.site, 2), expectItem())
          expectNoEvents()
        }
      }
    sync(site.incoming(), link)
  }
}
