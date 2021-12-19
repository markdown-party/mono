@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.alexandrepiveteau.echo.memory

import io.github.alexandrepiveteau.echo.*
import io.github.alexandrepiveteau.echo.core.causality.*
import io.github.alexandrepiveteau.echo.core.log.Event
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as I
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as O
import kotlin.random.Random
import kotlin.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.builtins.serializer

class MemoryExchangeIncomingTest {

  @Test
  fun only_Done_worksOnBufferedLink() = suspendTest {
    val echo = mutableSite<Unit>(123U.toSiteIdentifier())
    echo::receive.test {
      assertEquals(I.Ready, awaitItem())
      expectNoEvents()
    }
  }

  @Test
  fun noMessagesToIncomingWorks() = suspendTest {
    val echo = mutableSite<Unit>(123U.toSiteIdentifier()).buffer(RENDEZVOUS)
    val received = echo.receive(emptyFlow()).toList()
    assertEquals(emptyList(), received)
  }

  @Test
  fun advertisesOneEventAndCancelsIfRendezvousAndNotEmpty() = suspendTest {
    val seqno = 123U.toSequenceNumber()
    val site = 456U.toSiteIdentifier()
    val echo = mutableSite(site, EventIdentifier(seqno, site) to 42)
    echo::receive.test {
      assertEquals<Any>(I.Advertisement(site, seqno.inc()), awaitItem())
      assertEquals(I.Ready, awaitItem())
      expectNoEvents()
    }
  }

  @Test
  fun advertisesAllSitesInIncoming() = suspendTest {
    val count = 100
    val sites = List(count) { Random.nextSiteIdentifier() }
    val seqno = SequenceNumber.Min
    val events = sites.map { site -> EventIdentifier(seqno, site) to 123 }.toTypedArray()
    val echo = mutableSite(Random.nextSiteIdentifier(), *events).buffer(RENDEZVOUS)
    echo::receive.test {
      val received = mutableListOf<SiteIdentifier>()
      while (true) {
        when (val msg = awaitItem()) {
          is I.Advertisement -> received.add(msg.site)
          is I.Ready -> break
          else -> fail("Unexpected message $msg.")
        }
      }
      assertTrue(received.containsAll(sites))
      close()
      awaitComplete()
    }
  }

  @Test
  fun issuesOneEventOnRequest() = suspendTest {
    val site = 10U.toSiteIdentifier()
    val seqno = 150U.toSequenceNumber()
    val echo =
        mutableSite(
                SiteIdentifier.Min,
                EventIdentifier(seqno, site) to true,
            )
            .buffer(RENDEZVOUS)
    echo::receive.test {
      assertEquals(I.Advertisement(site, seqno.inc()), awaitItem())
      assertEquals(I.Ready, awaitItem())
      send(O.Acknowledge(site, seqno))
      send(O.Request(site, UInt.MAX_VALUE))
      assertEquals(
          I.Events(
              listOf(
                  Event(
                      seqno,
                      site,
                      DefaultSerializationFormat.encodeToByteArray(Boolean.serializer(), true),
                  )),
          ),
          awaitItem(),
      )
      close()
      awaitComplete()
    }
  }

  @Test
  fun anEventIsSentIfFirstRequestSizeIsZeroAndSecondIsNonZero() = suspendTest {
    val site = 123U.toSiteIdentifier()
    val seqno = 150U.toSequenceNumber()
    val echo = mutableSite(SiteIdentifier.Min, EventIdentifier(seqno, site) to true)
    echo::receive.test {
      assertEquals(I.Advertisement(site, seqno.inc()), awaitItem())
      assertEquals(I.Ready, awaitItem())
      send(O.Acknowledge(site, seqno))
      send(O.Request(site, count = 0U))
      send(O.Request(site, count = 1U))
      assertEquals(
          I.Events(
              listOf(
                  Event(
                      seqno,
                      site,
                      DefaultSerializationFormat.encodeToByteArray(Boolean.serializer(), true),
                  ),
              )),
          awaitItem(),
      )
      close()
      awaitComplete()
    }
  }
}
