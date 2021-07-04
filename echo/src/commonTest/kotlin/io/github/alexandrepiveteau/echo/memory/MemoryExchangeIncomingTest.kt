@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.alexandrepiveteau.echo.memory

import app.cash.turbine.test
import io.github.alexandrepiveteau.echo.*
import io.github.alexandrepiveteau.echo.core.causality.*
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
    val exchange =
        link<I, O> { incoming ->
          incoming.test {
            assertEquals(I.Ready, expectItem())
            expectNoEvents()
          }
        }
    sync(echo.incoming(), exchange)
  }

  @Test
  fun noMessagesToIncomingWorks() = suspendTest {
    val echo = mutableSite<Unit>(123U.toSiteIdentifier()).buffer(RENDEZVOUS)
    val received = echo.incoming().talk(emptyFlow()).toList()
    assertEquals(emptyList(), received)
  }

  @Test
  fun onlyDoneWorksOnOneBufferIncoming() = suspendTest {
    val echo = mutableSite<Unit>(123U.toSiteIdentifier())
    val exchange =
        link<I, O> { incoming ->
          incoming.test {
            assertEquals(I.Ready, expectItem())
            expectNoEvents()
          }
        }
    sync(echo.incoming(), exchange)
  }

  @Test
  fun advertisesOneEventAndCancelsIfRendezvousAndNotEmpty() = suspendTest {
    val seqno = 123U.toSequenceNumber()
    val site = 456U.toSiteIdentifier()
    val echo = mutableSite(site, EventIdentifier(seqno, site) to 42)
    val exchange =
        link<I, O> { incoming ->
          incoming.test {
            assertEquals<Any>(I.Advertisement(site, seqno.inc()), expectItem())
            assertEquals(I.Ready, expectItem())
            expectNoEvents()
          }
        }
    sync(echo.incoming(), exchange)
  }

  @Test
  fun advertisesAllSitesInIncoming() = suspendTest {
    val count = 100
    val sites = List(count) { Random.nextSiteIdentifier() }
    val seqno = SequenceNumber.Min
    val events = sites.map { site -> EventIdentifier(seqno, site) to 123 }.toTypedArray()
    val echo = mutableSite(Random.nextSiteIdentifier(), *events).buffer(RENDEZVOUS)
    val exchange =
        channelLink<I, O> { incoming ->
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
              assertNull(incoming.receiveCatching().getOrNull())
            }
            .buffer(RENDEZVOUS)
    sync(echo.incoming(), exchange)
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
    val exchange =
        channelLink<I, O> { incoming ->
              assertEquals(I.Advertisement(site, seqno.inc()), incoming.receive())
              assertEquals(I.Ready, incoming.receive())
              send(O.Acknowledge(site, seqno))
              send(O.Request(site, UInt.MAX_VALUE))
              assertEquals(
                  I.Event(
                      seqno,
                      site,
                      DefaultSerializationFormat.encodeToByteArray(Boolean.serializer(), true),
                  ),
                  incoming.receive(),
              )
              close()
              assertNull(incoming.receiveCatching().getOrNull())
            }
            .buffer(RENDEZVOUS)
    sync(echo.incoming(), exchange)
  }

  @Test
  fun anEventIsSentIfFirstRequestSizeIsZeroAndSecondIsNonZero() = suspendTest {
    val site = 123U.toSiteIdentifier()
    val seqno = 150U.toSequenceNumber()
    val echo = mutableSite(SiteIdentifier.Min, EventIdentifier(seqno, site) to true)
    val exchange =
        channelLink<I, O> { incoming ->
          assertEquals(I.Advertisement(site, seqno.inc()), incoming.receive())
          assertEquals(I.Ready, incoming.receive())
          send(O.Acknowledge(site, seqno))
          send(O.Request(site, count = 0U))
          send(O.Request(site, count = 1U))
          assertEquals(
              I.Event(
                  seqno,
                  site,
                  DefaultSerializationFormat.encodeToByteArray(Boolean.serializer(), true),
              ),
              incoming.receive(),
          )
          close()
          assertNull(incoming.receiveCatching().getOrNull())
        }
    sync(echo.incoming(), exchange)
  }
}
