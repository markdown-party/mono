package io.github.alexandrepiveteau.echo.site

import app.cash.turbine.test
import io.github.alexandrepiveteau.echo.*
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber.Companion.Min
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.builtins.serializer

class MutableSiteRequestTest {

  private fun encode(value: Int): ByteArray {
    return DefaultSerializationFormat.encodeToByteArray(Int.serializer(), value)
  }

  @Test
  fun noRequest_issuesNoEvents() = suspendTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val event = id to 1
    val site = site(event).buffer(Channel.RENDEZVOUS)
    val link =
        link<Inc, Out> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id.site, id.seqno.inc()), expectItem())
            assertEquals(Inc.Ready, expectItem())
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun onlyAck_issuesNoEvents() = suspendTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val event = id to 1
    val site = site(event).buffer(Channel.RENDEZVOUS)
    val link =
        link<Inc, Out> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id.site, id.seqno.inc()), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Acknowledge(id.site, Min))
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun onlyRequest_issuesNoEvents() = suspendTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val event = id to 1
    val site = site(event).buffer(Channel.RENDEZVOUS)
    val link =
        link<Inc, Out> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id.site, id.seqno.inc()), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Request(id.site, 10U))
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun ackThenRequest_zero_issuesNoEvent() = suspendTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val event = id to 1
    val site = site(event).buffer(Channel.RENDEZVOUS)
    val link =
        link<Inc, Out> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id.site, id.seqno.inc()), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Acknowledge(id.site, Min))
            emit(Out.Request(id.site, 0U))
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun ackThenRequest_issuesEvent() = suspendTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val event = id to 1
    val site = site(event).buffer(Channel.RENDEZVOUS)
    val link =
        link<Inc, Out> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id.site, id.seqno.inc()), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Acknowledge(id.site, Min))
            emit(Out.Request(id.site, 1U))
            assertEquals(Inc.Event(id.seqno, id.site, encode(1)), expectItem())
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun requestThenAck_issuesNoEvent() = suspendTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val event = id to 1
    val site = site(event).buffer(Channel.RENDEZVOUS)
    val link =
        link<Inc, Out> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id.site, id.seqno.inc()), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Request(id.site, 1U))
            emit(Out.Acknowledge(id.site, Min))
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun multipleRequest_sum() = suspendTest {
    val id = Random.nextSiteIdentifier()
    val id1 = EventIdentifier(Min + 0u, id)
    val id2 = EventIdentifier(Min + 1u, id)
    val event1 = id1 to 1
    val event2 = id2 to 2
    val site = site(event1, event2).buffer(Channel.RENDEZVOUS)
    val link =
        link<Inc, Out> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id, id2.seqno.inc()), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Acknowledge(id, Min))
            // Sum two requests before expecting items.
            emit(Out.Request(id, 1U))
            emit(Out.Request(id, 1U))
            assertEquals(Inc.Event(id1.seqno, id1.site, encode(1)), expectItem())
            assertEquals(Inc.Event(id2.seqno, id2.site, encode(2)), expectItem())
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun multipleRequest_interleaved_sum() = suspendTest {
    val id = Random.nextSiteIdentifier()
    val id1 = EventIdentifier(Min + 0u, id)
    val id2 = EventIdentifier(Min + 1u, id)
    val event1 = id1 to 1
    val event2 = id2 to 2
    val site = site(event1, event2).buffer(Channel.RENDEZVOUS)
    val link =
        link<Inc, Out> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id, id2.seqno.inc()), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Acknowledge(id, Min))
            // Interleave item reception and requests.
            emit(Out.Request(id, 1U))
            assertEquals(Inc.Event(id1.seqno, id1.site, encode(1)), expectItem())
            emit(Out.Request(id, 1U))
            assertEquals(Inc.Event(id2.seqno, id2.site, encode(2)), expectItem())
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }

  @Test
  fun ackThenRequest() = suspendTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val site = site<Int>().buffer(Channel.RENDEZVOUS)

    val link =
        link<Out, Inc> { inc ->
          inc.test {
            emit(Inc.Advertisement(id.site, id.seqno.inc()))
            emit(Inc.Ready)
            assertEquals(Out.Acknowledge(id.site, Min), expectItem())
            val request = expectItem() as Out.Request
            assertTrue(request.site == id.site && request.count >= 1U)
            expectNoEvents()
          }
        }
    sync(site.outgoing(), link)
  }

  @Test
  fun requestOverflow_isHandled() = suspendTest {
    val id = Random.nextSiteIdentifier()
    val id1 = EventIdentifier(Min + 0u, id)
    val id2 = EventIdentifier(Min + 1u, id)
    val event1 = id1 to 1
    val event2 = id2 to 2
    val site = site(event1, event2).buffer(Channel.RENDEZVOUS)
    val link =
        link<Inc, Out> { inc ->
          inc.test {
            assertEquals(Inc.Advertisement(id, id2.seqno.inc()), expectItem())
            assertEquals(Inc.Ready, expectItem())
            emit(Out.Acknowledge(id, Min))
            // Sum two requests such that their overflowing total is 0U before expecting items.
            emit(Out.Request(id, UInt.MAX_VALUE))
            emit(Out.Request(id, 1U))
            assertEquals(Inc.Event(id1.seqno, id1.site, encode(1)), expectItem())
            assertEquals(Inc.Event(id2.seqno, id2.site, encode(2)), expectItem())
            expectNoEvents()
          }
        }
    sync(site.incoming(), link)
  }
}
