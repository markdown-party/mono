package io.github.alexandrepiveteau.echo.site

import app.cash.turbine.test
import io.github.alexandrepiveteau.echo.*
import io.github.alexandrepiveteau.echo.core.causality.EventIdentifier
import io.github.alexandrepiveteau.echo.core.causality.SequenceNumber.Companion.Min
import io.github.alexandrepiveteau.echo.core.causality.nextSiteIdentifier
import io.github.alexandrepiveteau.echo.core.log.Event
import io.github.alexandrepiveteau.echo.protocol.Message.Incoming as Inc
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing as Out
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer

class MutableSiteRequestTest {

  private fun encode(value: Int): ByteArray {
    return DefaultSerializationFormat.encodeToByteArray(Int.serializer(), value)
  }

  @Test
  fun noRequest_issuesNoEvents() = runTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val event = id to 1
    val site = site(event).buffer(Channel.RENDEZVOUS)
    site::receive.test {
      assertEquals(Inc.Advertisement(id.site, id.seqno.inc()), awaitItem())
      assertEquals(Inc.Ready, awaitItem())
      expectNoEvents()
    }
  }

  @Test
  fun onlyAck_issuesNoEvents() = runTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val event = id to 1
    val site = site(event).buffer(Channel.RENDEZVOUS)
    site::receive.test {
      assertEquals(Inc.Advertisement(id.site, id.seqno.inc()), awaitItem())
      assertEquals(Inc.Ready, awaitItem())
      send(Out.Acknowledge(id.site, Min))
      expectNoEvents()
    }
  }

  @Test
  fun onlyRequest_issuesNoEvents() = runTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val event = id to 1
    val site = site(event).buffer(Channel.RENDEZVOUS)
    val link: (Flow<Inc>) -> Flow<Out> = { inc ->
      flow {
        inc.test {
          assertEquals(Inc.Advertisement(id.site, id.seqno.inc()), awaitItem())
          assertEquals(Inc.Ready, awaitItem())
          emit(Out.Request(id.site, 10U))
          expectNoEvents()
        }
      }
    }
    sync(site::receive, link)
  }

  @Test
  fun ackThenRequest_zero_issuesNoEvent() = runTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val event = id to 1
    val site = site(event).buffer(Channel.RENDEZVOUS)
    site::receive.test {
      assertEquals(Inc.Advertisement(id.site, id.seqno.inc()), awaitItem())
      assertEquals(Inc.Ready, awaitItem())
      send(Out.Acknowledge(id.site, Min))
      send(Out.Request(id.site, 0U))
      expectNoEvents()
    }
  }

  @Test
  fun ackThenRequest_issuesEvent() = runTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val event = id to 1
    val site = site(event).buffer(Channel.RENDEZVOUS)
    site::receive.test {
      assertEquals(Inc.Advertisement(id.site, id.seqno.inc()), awaitItem())
      assertEquals(Inc.Ready, awaitItem())
      send(Out.Acknowledge(id.site, Min))
      send(Out.Request(id.site, 1U))
      assertEquals(Inc.Events(listOf(Event(id.seqno, id.site, encode(1)))), awaitItem())
      expectNoEvents()
    }
  }

  @Test
  fun requestThenAck_issuesNoEvent() = runTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val event = id to 1
    val site = site(event).buffer(Channel.RENDEZVOUS)
    val link: (Flow<Inc>) -> Flow<Out> = { inc ->
      flow {
        inc.test {
          assertEquals(Inc.Advertisement(id.site, id.seqno.inc()), awaitItem())
          assertEquals(Inc.Ready, awaitItem())
          emit(Out.Request(id.site, 1U))
          emit(Out.Acknowledge(id.site, Min))
          expectNoEvents()
        }
      }
    }
    sync(site::receive, link)
  }

  @Test
  fun multipleRequest_sum() = runTest {
    val id = Random.nextSiteIdentifier()
    val id1 = EventIdentifier(Min + 0u, id)
    val id2 = EventIdentifier(Min + 1u, id)
    val event1 = id1 to 1
    val event2 = id2 to 2
    val site = site(event1, event2).buffer(Channel.RENDEZVOUS)
    site::receive.test {
      assertEquals(Inc.Advertisement(id, id2.seqno.inc()), awaitItem())
      assertEquals(Inc.Ready, awaitItem())
      send(Out.Acknowledge(id, Min))
      // Sum two requests before expecting items.
      send(Out.Request(id, 1U))
      send(Out.Request(id, 1U))
      assertEquals(Inc.Events(listOf(Event(id1.seqno, id1.site, encode(1)))), awaitItem())
      assertEquals(Inc.Events(listOf(Event(id2.seqno, id2.site, encode(2)))), awaitItem())
      expectNoEvents()
    }
  }

  @Test
  fun multipleRequest_interleaved_sum() = runTest {
    val id = Random.nextSiteIdentifier()
    val id1 = EventIdentifier(Min + 0u, id)
    val id2 = EventIdentifier(Min + 1u, id)
    val event1 = id1 to 1
    val event2 = id2 to 2
    val site = site(event1, event2).buffer(Channel.RENDEZVOUS)
    site::receive.test {
      assertEquals(Inc.Advertisement(id, id2.seqno.inc()), awaitItem())
      assertEquals(Inc.Ready, awaitItem())
      send(Out.Acknowledge(id, Min))
      // Interleave item reception and requests.
      send(Out.Request(id, 1U))
      assertEquals(Inc.Events(listOf(Event(id1.seqno, id1.site, encode(1)))), awaitItem())
      send(Out.Request(id, 1U))
      assertEquals(Inc.Events(listOf(Event(id2.seqno, id2.site, encode(2)))), awaitItem())
      expectNoEvents()
    }
  }

  @Test
  fun ackThenRequest() = runTest {
    val id = EventIdentifier(Min, Random.nextSiteIdentifier())
    val site = site<Int>().buffer(Channel.RENDEZVOUS)
    site::send.test {
      send(Inc.Advertisement(id.site, id.seqno.inc()))
      send(Inc.Ready)
      assertEquals(Out.Acknowledge(id.site, Min), awaitItem())
      val request = awaitItem() as Out.Request
      assertTrue(request.site == id.site && request.count >= 1U)
      expectNoEvents()
    }
  }

  @Test
  fun requestOverflow_isHandled() = runTest {
    val id = Random.nextSiteIdentifier()
    val id1 = EventIdentifier(Min + 0u, id)
    val id2 = EventIdentifier(Min + 1u, id)
    val event1 = id1 to 1
    val event2 = id2 to 2
    val site = site(event1, event2).buffer(Channel.RENDEZVOUS)
    site::receive.test {
      assertEquals(Inc.Advertisement(id, id2.seqno.inc()), awaitItem())
      assertEquals(Inc.Ready, awaitItem())
      send(Out.Acknowledge(id, Min))
      // Sum two requests such that their overflowing total is 0U before expecting items.
      send(Out.Request(id, UInt.MAX_VALUE))
      send(Out.Request(id, 1U))
      assertEquals(
          // The two events are available when the request is performed.
          Inc.Events(
              listOf(
                  Event(id1.seqno, id1.site, encode(1)),
                  Event(id2.seqno, id2.site, encode(2)),
              )),
        awaitItem())
      expectNoEvents()
    }
  }
}
