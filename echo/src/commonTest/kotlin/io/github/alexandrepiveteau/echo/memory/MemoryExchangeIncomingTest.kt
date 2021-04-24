@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.alexandrepiveteau.echo.memory

import app.cash.turbine.test
import io.github.alexandrepiveteau.echo.*
import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.logs.persistentEventLogOf
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as I
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as O
import kotlin.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull

class MemoryExchangeIncomingTest {

  @Test
  fun only_Done_worksOnBufferedLink() = suspendTest {
    val echo = mutableSite<Nothing>(SiteIdentifier(123))
    val exchange =
        link<I<Nothing>, O<Nothing>> { incoming ->
          incoming.test {
            assertEquals(I.Ready, expectItem())
            expectNoEvents()
          }
        }
    sync(echo.incoming(), exchange)
  }

  @Test
  fun noMessagesToIncomingWorks() = suspendTest {
    val echo = mutableSite<Nothing>(SiteIdentifier(123)).buffer(RENDEZVOUS)
    val received = echo.incoming().talk(emptyFlow()).toList()
    assertEquals(emptyList(), received)
  }

  @Test
  fun onlyDoneWorksOnOneBufferIncoming() = suspendTest {
    val echo = mutableSite<Nothing>(SiteIdentifier(123))
    val exchange =
        link<I<Nothing>, O<Nothing>> { incoming ->
          incoming.test {
            assertEquals(I.Ready, expectItem())
            expectNoEvents()
          }
        }
    sync(echo.incoming(), exchange)
  }

  @Test
  fun advertisesOneEventAndCancelsIfRendezvousAndNotEmpty() = suspendTest {
    val seqno = SequenceNumber(123U)
    val site = SiteIdentifier(456)
    val log = persistentEventLogOf(EventIdentifier(seqno, site) to 42)
    val echo = mutableSite(site, log)
    val exchange =
        link<I<Int>, O<Int>> { incoming ->
          incoming.test {
            assertEquals<Any>(I.Advertisement(site), expectItem())
            assertEquals(I.Ready, expectItem())
            expectNoEvents()
          }
        }
    sync(echo.incoming(), exchange)
  }

  @Test
  fun advertisesAllSitesInIncoming() = suspendTest {
    val count = 100
    val sites = List(count) { SiteIdentifier.random() }
    val seqno = SequenceNumber.Zero
    val events = sites.map { site -> EventIdentifier(seqno, site) to 123 }
    val log = persistentEventLogOf(*events.toTypedArray())
    val echo = mutableSite(SiteIdentifier.random(), log).buffer(RENDEZVOUS)
    val exchange =
        channelLink<I<Int>, O<Int>> { incoming ->
              val received = mutableListOf<SiteIdentifier>()
              while (true) {
                when (val msg = incoming.receive()) {
                  is I.Advertisement -> received.add(msg.site)
                  is I.Ready -> break
                  else -> fail("Unexpected message $msg.")
                }
              }
              assertTrue(received.containsAll(sites))
              close()
              assertNull(incoming.receiveOrNull())
            }
            .buffer(RENDEZVOUS)
    sync(echo.incoming(), exchange)
  }

  @Test
  fun issuesOneEventOnRequest() = suspendTest {
    val site = SiteIdentifier(10)
    val seqno = SequenceNumber(150U)
    val events = persistentEventLogOf(EventIdentifier(seqno, site) to true)
    val echo = mutableSite(SiteIdentifier(0), events).buffer(RENDEZVOUS)
    val exchange =
        channelLink<I<Boolean>, O<Boolean>> { incoming ->
              assertEquals(I.Advertisement(site), incoming.receive())
              assertEquals(I.Ready, incoming.receive())
              send(O.Request(seqno, site = site))
              assertEquals(I.Event(seqno, site, true), incoming.receive())
              close()
              assertNull(incoming.receiveOrNull())
            }
            .buffer(RENDEZVOUS)
    sync(echo.incoming(), exchange)
  }

  @Test
  fun noEventIsSentIfRequestSizeIsZero() = suspendTest {
    val site = SiteIdentifier(123)
    val seqno = SequenceNumber(150U)
    val events = persistentEventLogOf(EventIdentifier(seqno, site) to true)
    val echo = mutableSite(SiteIdentifier(0), events)
    val exchange =
        channelLink<I<Boolean>, O<Boolean>> { incoming ->
          assertEquals(I.Advertisement(site), incoming.receive())
          assertEquals(I.Ready, incoming.receive())
          send(O.Request(seqno, site, count = 0))
          incoming.receive()
          fail("incoming.receive() should have timeout.")
        }
    val nullIfFailure = withTimeoutOrNull(1000) { sync(echo.incoming(), exchange) }
    assertNull(nullIfFailure)
  }

  @Test
  fun anEventIsSentIfFirstRequestSizeIsZeroAndSecondIsNonZero() = suspendTest {
    val site = SiteIdentifier(123)
    val seqno = SequenceNumber(150U)
    val events = persistentEventLogOf(EventIdentifier(seqno, site) to true)
    val echo = mutableSite(SiteIdentifier(0), events)
    val exchange =
        channelLink<I<Boolean>, O<Boolean>> { incoming ->
          assertEquals(I.Advertisement(site), incoming.receive())
          assertEquals(I.Ready, incoming.receive())
          send(O.Request(seqno, site, count = 0))
          send(O.Request(seqno, site, count = 1))
          assertEquals(I.Event(seqno, site, true), incoming.receive())
          close()
          assertNull(incoming.receiveOrNull())
        }
    sync(echo.incoming(), exchange)
  }
}
