@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.alexandrepiveteau.echo.memory

import io.github.alexandrepiveteau.echo.buffer
import io.github.alexandrepiveteau.echo.causal.EventIdentifier
import io.github.alexandrepiveteau.echo.causal.SequenceNumber
import io.github.alexandrepiveteau.echo.causal.SiteIdentifier
import io.github.alexandrepiveteau.echo.channelLink
import io.github.alexandrepiveteau.echo.logs.persistentEventLogOf
import io.github.alexandrepiveteau.echo.mutableSite
import io.github.alexandrepiveteau.echo.protocol.Message.V1.Incoming as I
import io.github.alexandrepiveteau.echo.protocol.Message.V1.Outgoing as O
import io.github.alexandrepiveteau.echo.sync
import kotlin.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class MemoryExchangeIncomingTest {

  @Test
  fun `Only Done works on buffered incoming`() = runBlocking {
    val echo = mutableSite<Nothing>(SiteIdentifier(123))
    val exchange =
        channelLink<I<Nothing>, O<Nothing>> { incoming ->
          assertTrue(incoming.receive() is I.Ready)
          send(O.Done)
          assertTrue(incoming.receive() is I.Done)
          assertNull(incoming.receiveOrNull())
        }
    sync(echo.incoming(), exchange)
  }

  @Test
  fun `No messages to incoming works`() = runBlocking {
    val echo = mutableSite<Nothing>(SiteIdentifier(123)).buffer(RENDEZVOUS)
    val received = echo.incoming().talk(emptyFlow()).toList()
    assertEquals(listOf(I.Ready, I.Done), received)
  }

  @Test
  fun `Only Done works on 1-buffer incoming`() = runBlocking {
    val echo = mutableSite<Nothing>(SiteIdentifier(123)).buffer(RENDEZVOUS)
    val exchange =
        channelLink<I<Nothing>, O<Nothing>> { incoming ->
              assertTrue(incoming.receive() is I.Ready)
              send(O.Done)
              assertTrue(incoming.receive() is I.Done)
              assertNull(incoming.receiveOrNull())
            }
            .buffer(RENDEZVOUS)
    sync(echo.incoming(), exchange)
  }

  @Test
  fun `Advertises one event and cancels if rendezvous and not empty`() = runBlocking {
    val seqno = SequenceNumber(123U)
    val site = SiteIdentifier(456)
    val log = persistentEventLogOf(EventIdentifier(seqno, site) to 42)
    val echo = mutableSite(site, log).buffer(RENDEZVOUS)
    val exchange =
        channelLink<I<Int>, O<Int>> { incoming ->
              assertEquals(I.Advertisement(site), incoming.receive())
              assertEquals(I.Ready, incoming.receive())
              send(O.Done)
              assertTrue(incoming.receive() is I.Done)
              assertNull(incoming.receiveOrNull())
            }
            .buffer(RENDEZVOUS)
    sync(echo.incoming(), exchange)
  }

  @Test
  fun `Advertises all sites in incoming`() = runBlocking {
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
              send(O.Done)
              incoming.receive() as I.Done
              incoming.receiveOrNull()
            }
            .buffer(RENDEZVOUS)
    sync(echo.incoming(), exchange)
  }

  @Test
  fun `Issues one event on request`() = runBlocking {
    val site = SiteIdentifier(10)
    val seqno = SequenceNumber(150U)
    val events = persistentEventLogOf(EventIdentifier(seqno, site) to true)
    val echo = mutableSite(SiteIdentifier(0), events).buffer(RENDEZVOUS)
    val exchange =
        channelLink<I<Boolean>, O<Boolean>> { incoming ->
              assertEquals(I.Advertisement(site), incoming.receive())
              assertEquals(I.Ready, incoming.receive())
              send(O.Request(seqno, seqno, site = site))
              assertEquals(I.Event(seqno, site, true), incoming.receive())
              send(O.Done)
              assertEquals(I.Done, incoming.receive())
              assertNull(incoming.receiveOrNull())
            }
            .buffer(RENDEZVOUS)
    sync(echo.incoming(), exchange)
  }

  @Test
  fun `No event is sent if request size is zero`() = runBlocking {
    val site = SiteIdentifier(123)
    val seqno = SequenceNumber(150U)
    val events = persistentEventLogOf(EventIdentifier(seqno, site) to true)
    val echo = mutableSite(SiteIdentifier(0), events)
    val exchange =
        channelLink<I<Boolean>, O<Boolean>> { incoming ->
          assertEquals(I.Advertisement(site), incoming.receive())
          assertEquals(I.Ready, incoming.receive())
          send(O.Request(seqno, seqno, site, count = 0))
          incoming.receive()
          fail("incoming.receive() should have timeout.")
        }
    val nullIfFailure = withTimeoutOrNull(1000) { sync(echo.incoming(), exchange) }
    assertNull(nullIfFailure)
  }

  @Test
  fun `An event is sent if first request size is zero and second is non-zero`() = runBlocking {
    val site = SiteIdentifier(123)
    val seqno = SequenceNumber(150U)
    val events = persistentEventLogOf(EventIdentifier(seqno, site) to true)
    val echo = mutableSite(SiteIdentifier(0), events)
    val exchange =
        channelLink<I<Boolean>, O<Boolean>> { incoming ->
          assertEquals(I.Advertisement(site), incoming.receive())
          assertEquals(I.Ready, incoming.receive())
          send(O.Request(seqno, seqno, site, count = 0))
          send(O.Request(seqno, seqno, site, count = 1))
          assertEquals(I.Event(seqno, site, true), incoming.receive())
          send(O.Done)
          assertEquals(I.Done, incoming.receive())
          assertNull(incoming.receiveOrNull())
        }
    sync(echo.incoming(), exchange)
  }
}
