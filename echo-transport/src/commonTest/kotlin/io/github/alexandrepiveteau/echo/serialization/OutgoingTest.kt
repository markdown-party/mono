package io.github.alexandrepiveteau.echo.serialization

import io.github.alexandrepiveteau.echo.causal.toSequenceNumber
import io.github.alexandrepiveteau.echo.causal.toSiteIdentifier
import io.github.alexandrepiveteau.echo.protocol.Message.Outgoing
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OutgoingTest {

  @Test
  fun testAcknowledgeDecoding() {
    val json =
        """
            { "type": "acknowledge"
            , "nextSeqno": 123
            , "site": "00000000"
            }
        """
    val decoded = Json.decodeFromString(Outgoing.serializer<Nothing>(), json)
    assertEquals(
        Outgoing.Acknowledge(
            nextSeqno = 123u.toSequenceNumber(),
            site = Int.MIN_VALUE.toSiteIdentifier(),
        ),
        decoded,
    )
  }

  @Test
  fun testRequestDecoding() {
    val json =
        """
            { "type": "request"
            , "site": "00000000"
            , "count": 42
            }
        """
    val decoded = Json.decodeFromString(Outgoing.serializer<Nothing>(), json)
    assertEquals(
        Outgoing.Request(
            site = Int.MIN_VALUE.toSiteIdentifier(),
            count = 42U,
        ),
        decoded,
    )
  }
}
